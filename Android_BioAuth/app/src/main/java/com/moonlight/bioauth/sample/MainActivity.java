package com.moonlight.bioauth.sample;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity {


    // finger
    private static final String KEY_NAME = "example_key";
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
    private FingerprintManager.CryptoObject cryptoObject;

    // BioPrompt
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricManager manager;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = BiometricManager.from(this);


        Button biometricLoginButton = findViewById(R.id.main_auth_pinger_print_btn);
        biometricLoginButton.setOnClickListener(view -> {

            if (isUsingFingerPrintAPI() == true) {
                generateKey();
                try {
                    if (cipherInit()) {
                        cryptoObject = new FingerprintManager.CryptoObject(cipher);
                        //  FingerprintHandler ??????
                        FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
                        fingerprintHandler.startAuth(fingerprintManager, cryptoObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "?????? ??????????????? ????????? ????????? ???????????? ????????????.\n"+"reason : "+e.toString(), Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "?????? ??????????????? ????????? ????????? ???????????? ????????????.", Toast.LENGTH_LONG).show();
            }
        });


        Button biometricButton = findViewById(R.id.main_auth_print_btn);
        biometricButton.setOnClickListener(view -> {

            if (isUsingBiometricPromptAPI() == true) {
                BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder();

                promptBuilder.setTitle("Biometric login for my app");
                promptBuilder.setSubtitle("Log in using your biometric credential");
                promptBuilder.setNegativeButtonText("Use account password");

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //  ?????? ?????? ap?????? android 11?????? ??????
//                    promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
//                }

                promptInfo = promptBuilder.build();
                executor = ContextCompat.getMainExecutor(this);
                biometricPrompt = new BiometricPrompt(MainActivity.this,
                        executor, new BiometricPromptHandler(MainActivity.this));
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(this, "?????? ??????????????? ????????? ????????? ???????????? ????????????.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // fingerPrint ???????????? ?????? ??????
    private boolean isUsingFingerPrintAPI() {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // Marshmallow?????? ?????? ?????? ??????
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (fingerprintManager.isHardwareDetected() == false) { //Manifest??? Fingerprint ???????????? ???????????? ????????? ?????????.
                Toast.makeText(this, "??????????????? ???????????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
                result = false;
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "?????? ????????? ????????? ????????? ?????????.", Toast.LENGTH_LONG).show();
                result = false;
            } else if (fingerprintManager.hasEnrolledFingerprints() == false) {
                Toast.makeText(this, "????????? ??????????????? ????????????.", Toast.LENGTH_LONG).show();
                result = false;
            } else {    //  ?????? ?????? ????????????
                result = true;
            }
        }
        return result;
    }


    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (
                NoSuchPaddingException | KeyStoreException | java.security.cert.CertificateException
                        | UnrecoverableKeyException | IOException
                        | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    //Key Generator
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  biometricPromptAPI ???????????? ?????? ??????
    private boolean isUsingBiometricPromptAPI() {
        boolean result = false;
        if (manager != null) {
            switch (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                case BiometricManager.BIOMETRIC_SUCCESS: {   //  ?????? ?????? ??????
                    result = true;
                    Log.d("MainActivity", "Application can authenticate with biometrics.");
                    break;
                }
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE: { //  ???????????? ?????? ????????? ???????????? ?????? ??????
                    result = false;
                    Log.d("MainActivity", "Biometric facility is not available in this device.");
                    break;
                }
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE: {  //
                    result = false;
                    Log.d("MainActivity", "Biometric facility is currently not available");
                    break;
                }
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED: {   //  ?????? ?????? ????????? ???????????? ?????? ??????
                    result = false;
                    Log.d("MainActivity", "Any biometric credential is not added in this device.");
                    break;
                }
                default: {   //   ?????? ??????
                    result = false;
                    Log.d("MainActivity", "Fail Biometric facility");
                    break;
                }
            }
        }
        return result;
    }

}
