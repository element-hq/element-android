/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.auth.registration

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable

interface RegistrationWizard {

    fun getRegistrationFlow(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun createAccount(userName: String, password: String, initialDeviceDisplayName: String?, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun performReCaptcha(response: String, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun acceptTerms(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun dummy(callback: MatrixCallback<RegistrationResult>): Cancelable

    fun addEmail(email: String, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun addMsisdn(msisdn: String, callback: MatrixCallback<RegistrationResult>): Cancelable

    fun confirmMsisdn(code: String, callback: MatrixCallback<RegistrationResult>): Cancelable

}
