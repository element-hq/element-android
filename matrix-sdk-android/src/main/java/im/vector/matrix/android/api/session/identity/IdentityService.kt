/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.identity

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable

/**
 * Provides access to the identity server configuration and services identity server can provide
 */
interface IdentityService {

    /**
     * Return the default identity server of the homeserver (using Wellknown request)
     */
    fun getDefaultIdentityServer(callback: MatrixCallback<String?>): Cancelable

    fun getCurrentIdentityServer(): String?

    /**
     * Update the identity server url.
     * @param url the new url. Set to null to disconnect from the identity server
     * @param callback will notify the user is change successful. The String will be the final url of the identity server, or null.
     * The SDK can append "https://" for instance.
     */
    fun setNewIdentityServer(url: String?, callback: MatrixCallback<String?>): Cancelable

    /**
     * This will ask the identity server to send an email or an SMS to let the user confirm he owns the ThreePid,
     * and then the threePid will be associated with the matrix account
     */
    fun startBindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * This will perform the actual association of ThreePid and Matrix account
     */
    fun finalizeBindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * @param code the code sent to the user phone number
     */
    fun submitValidationToken(pid: ThreePid, code: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * The request will actually be done on the homeserver
     */
    fun unbindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Search MatrixId of users providing email and phone numbers
     */
    fun lookUp(threePids: List<ThreePid>, callback: MatrixCallback<List<FoundThreePid>>): Cancelable

    fun addListener(listener: IdentityServiceListener)
    fun removeListener(listener: IdentityServiceListener)
}
