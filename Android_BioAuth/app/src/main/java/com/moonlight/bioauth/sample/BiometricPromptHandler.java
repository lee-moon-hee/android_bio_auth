package com.moonlight.bioauth.sample;

import android.content.Context;
import android.widget.Toast;

import androidx.biometric.BiometricPrompt;

/**
 * BiometricPrompt callback class
 */
public class BiometricPromptHandler extends BiometricPrompt.AuthenticationCallback {

    private Context context = null;

    public BiometricPromptHandler(Context context) {
        this.context = context;
    }

    @Override
    public void onAuthenticationError(int errorCode,
                                      CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        Toast.makeText(context,
                "ErrorCode: " + errorCode +
                        "\nAuthentication error: " + errString, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
//                Arrays.toString(encryptedInfo);
        Toast.makeText(context, "result : Authentication succeeded", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        Toast.makeText(context, "Authentication failed",
                Toast.LENGTH_SHORT)
                .show();
    }
}
