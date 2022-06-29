/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.pin.lockscreen.biometrics

import androidx.biometric.BiometricPrompt

/**
 * Wrapper for [BiometricPrompt.AuthenticationCallback] errors.
 */
class BiometricAuthError(val code: Int, message: String) : Throwable(message) {
    /**
     * This error disables Biometric authentication, either temporarily or permanently.
     */
    val isAuthDisabledError: Boolean get() = code in LOCKOUT_ERROR_CODES

    /**
     * This error permanently disables Biometric authentication.
     */
    val isAuthPermanentlyDisabledError: Boolean get() = code == BiometricPrompt.ERROR_LOCKOUT_PERMANENT

    companion object {
       private val LOCKOUT_ERROR_CODES = arrayOf(BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT)
    }
}
