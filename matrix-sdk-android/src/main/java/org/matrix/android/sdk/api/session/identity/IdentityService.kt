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

package org.matrix.android.sdk.api.session.identity

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable

/**
 * Provides access to the identity server configuration and services identity server can provide
 */
interface IdentityService {
    /**
     * Return the default identity server of the user, which may have been provided at login time by the homeserver,
     * or by the Well-known setup of the homeserver
     * It may be different from the current configured identity server
     */
    fun getDefaultIdentityServer(): String?

    /**
     * Return the current identity server URL used by this account. Returns null if no identity server is configured.
     */
    fun getCurrentIdentityServerUrl(): String?

    /**
     * Check if the identity server is valid
     * See https://matrix.org/docs/spec/identity_service/latest#status-check
     * RiotX SDK only supports identity server API v2
     */
    fun isValidIdentityServer(url: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the identity server url.
     * If successful, any previous identity server will be disconnected.
     * In case of error, any previous identity server will remain configured.
     * @param url the new url.
     * @param callback will notify the user if change is successful. The String will be the final url of the identity server.
     * The SDK can prepend "https://" for instance.
     */
    fun setNewIdentityServer(url: String, callback: MatrixCallback<String>): Cancelable

    /**
     * Disconnect (logout) from the current identity server
     */
    fun disconnect(callback: MatrixCallback<Unit>): Cancelable

    /**
     * This will ask the identity server to send an email or an SMS to let the user confirm he owns the ThreePid
     */
    fun startBindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * This will cancel a pending binding of threePid.
     */
    fun cancelBindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * This will ask the identity server to send an new email or a new SMS to let the user confirm he owns the ThreePid
     */
    fun sendAgainValidationCode(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Submit the code that the identity server has sent to the user (in email or SMS)
     * Once successful, you will have to call [finalizeBindThreePid]
     * @param code the code sent to the user
     */
    fun submitValidationToken(threePid: ThreePid, code: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * This will perform the actual association of ThreePid and Matrix account
     */
    fun finalizeBindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Unbind a threePid
     * The request will actually be done on the homeserver
     */
    fun unbindThreePid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Search MatrixId of users providing email and phone numbers
     */
    fun lookUp(threePids: List<ThreePid>, callback: MatrixCallback<List<FoundThreePid>>): Cancelable

    /**
     * Get the status of the current user's threePid
     * A lookup will be performed, but also pending binding state will be restored
     *
     * @param threePids the list of threePid the user owns (retrieved form the homeserver)
     * @param callback onSuccess will be called with a map of ThreePid -> SharedState
     */
    fun getShareStatus(threePids: List<ThreePid>, callback: MatrixCallback<Map<ThreePid, SharedState>>): Cancelable

    fun addListener(listener: IdentityServiceListener)
    fun removeListener(listener: IdentityServiceListener)
}
