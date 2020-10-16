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

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable

interface RegistrationWizard {

    fun getRegistrationFlow(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun createAccount(userName: String, password: String, initialDeviceDisplayName: String?, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun performReCaptcha(response: String, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun acceptTerms(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun dummy(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun addThreePid(threePid: RegisterThreePid, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun sendAgainThreePid(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun handleValidateThreePid(code: String, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun checkIfEmailHasBeenValidated(delayMillis: Long, callback: MatrixCallback<RegistrationResult>): Cancelable

    val currentThreePid: String?

    // True when login and password has been sent with success to the homeserver
    val isRegistrationStarted: Boolean
}
