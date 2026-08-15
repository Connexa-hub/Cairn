package com.cairn.app.data.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executor

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * Gates access to the vault behind biometrics, falling back to the device
 * PIN/pattern/password (never a Cairn-specific PIN stored anywhere) via
 * BIOMETRIC_STRONG | DEVICE_CREDENTIAL. Successful auth here is what
 * authorizes [DbKeyManager] to be read.
 */
class BiometricAuthManager(private val activity: FragmentActivity, private val executor: Executor) {

    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate() = callbackFlow {
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    trySend(AuthResult.Success)
                    close()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    trySend(AuthResult.Error(errString.toString()))
                    close()
                }

                override fun onAuthenticationFailed() {
                    trySend(AuthResult.Error("Authentication failed"))
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cairn")
            .setSubtitle("Your archive is encrypted on this device")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
        awaitClose { }
    }
}
