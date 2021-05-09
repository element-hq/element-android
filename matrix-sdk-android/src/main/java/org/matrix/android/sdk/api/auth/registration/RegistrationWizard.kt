/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.auth.registration

interface RegistrationWizard {

    suspend fun getRegistrationFlow(): RegistrationResult

    suspend fun createAccount(userName: String?,
                              password: String?,
                              initialDeviceDisplayName: String?): RegistrationResult

    suspend fun performReCaptcha(response: String): RegistrationResult

    suspend fun acceptTerms(): RegistrationResult

    suspend fun dummy(): RegistrationResult

    suspend fun addThreePid(threePid: RegisterThreePid): RegistrationResult

    suspend fun sendAgainThreePid(): RegistrationResult

    suspend fun handleValidateThreePid(code: String): RegistrationResult

    suspend fun checkIfEmailHasBeenValidated(delayMillis: Long): RegistrationResult

    suspend fun registrationAvailable(userName: String): RegistrationAvailability

    val currentThreePid: String?

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
}
