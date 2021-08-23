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
     * Matrix Android SDK2 only supports identity server API v2
     */
    suspend fun isValidIdentityServer(url: String)

    /**
     * Update the identity server url.
     * If successful, any previous identity server will be disconnected.
     * In case of error, any previous identity server will remain configured.
     * @param url the new url.
     * @return The String will be the final url of the identity server.
     * The SDK can prepend "https://" for instance.
     */
    suspend fun setNewIdentityServer(url: String): String

    /**
     * Disconnect (logout) from the current identity server
     */
    suspend fun disconnect()

    /**
     * This will ask the identity server to send an email or an SMS to let the user confirm he owns the ThreePid
     */
    suspend fun startBindThreePid(threePid: ThreePid)

    /**
     * This will cancel a pending binding of threePid.
     */
    suspend fun cancelBindThreePid(threePid: ThreePid)

    /**
     * This will ask the identity server to send an new email or a new SMS to let the user confirm he owns the ThreePid
     */
    suspend fun sendAgainValidationCode(threePid: ThreePid)

    /**
     * Submit the code that the identity server has sent to the user (in email or SMS)
     * Once successful, you will have to call [finalizeBindThreePid]
     * @param code the code sent to the user
     */
    suspend fun submitValidationToken(threePid: ThreePid, code: String)

    /**
     * This will perform the actual association of ThreePid and Matrix account
     */
    suspend fun finalizeBindThreePid(threePid: ThreePid)

    /**
     * Unbind a threePid
     * The request will actually be done on the homeserver
     */
    suspend fun unbindThreePid(threePid: ThreePid)

    /**
     * Search MatrixId of users providing email and phone numbers
     * Note the the user consent has to be set to true, or it will throw a UserConsentNotProvided failure
     * Application has to explicitly ask for the user consent, and the answer can be stored using [setUserConsent]
     * Please see https://support.google.com/googleplay/android-developer/answer/9888076?hl=en for more details.
     */
    suspend fun lookUp(threePids: List<ThreePid>): List<FoundThreePid>

    /**
     * Return the current user consent for the current identity server, which has been stored using [setUserConsent].
     * If [setUserConsent] has not been called, the returned value will be false.
     * Note that if the identity server is changed, the user consent is reset to false.
     * @return the value stored using [setUserConsent] or false if [setUserConsent] has never been called, or if the identity server
     *         has been changed
     */
    fun getUserConsent(): Boolean

    /**
     * Set the user consent to the provided value. Application MUST explicitly ask for the user consent to send their private data
     * (email and phone numbers) to the identity server.
     * Please see https://support.google.com/googleplay/android-developer/answer/9888076?hl=en for more details.
     * @param newValue true if the user explicitly give their consent, false if the user wants to revoke their consent.
     */
    fun setUserConsent(newValue: Boolean)

    /**
     * Get the status of the current user's threePid
     * A lookup will be performed, but also pending binding state will be restored
     *
     * @param threePids the list of threePid the user owns (retrieved form the homeserver)
     * @return a map of ThreePid -> SharedState
     */
    suspend fun getShareStatus(threePids: List<ThreePid>): Map<ThreePid, SharedState>

    fun addListener(listener: IdentityServiceListener)
    fun removeListener(listener: IdentityServiceListener)
}
