/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.auth

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.login.LoginWizard
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.Cancelable

/**
 * This interface defines methods to authenticate or to create an account to a matrix server.
 */
interface AuthenticationService {
    /**
     * Request the supported login flows for this homeserver.
     * This is the first method to call to be able to get a wizard to login or the create an account
     */
    fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<LoginFlowResult>): Cancelable

    /**
     * Request the supported login flows for the corresponding sessionId.
     */
    fun getLoginFlowOfSession(sessionId: String, callback: MatrixCallback<LoginFlowResult>): Cancelable

    /**
     * Get a SSO url
     */
    fun getSsoUrl(redirectUrl: String, deviceId: String?, providerId: String?): String?

    /**
     * Get the sign in or sign up fallback URL
     */
    fun getFallbackUrl(forSignIn: Boolean, deviceId: String?): String?

    /**
     * Return a LoginWizard, to login to the homeserver. The login flow has to be retrieved first.
     */
    fun getLoginWizard(): LoginWizard

    /**
     * Return a RegistrationWizard, to create an matrix account on the homeserver. The login flow has to be retrieved first.
     */
    fun getRegistrationWizard(): RegistrationWizard

    /**
     * True when login and password has been sent with success to the homeserver
     */
    val isRegistrationStarted: Boolean

    /**
     * Cancel pending login or pending registration
     */
    fun cancelPendingLoginOrRegistration()

    /**
     * Reset all pending settings, including current HomeServerConnectionConfig
     */
    fun reset()

    /**
     * Check if there is an authenticated [Session].
     * @return true if there is at least one active session.
     */
    fun hasAuthenticatedSessions(): Boolean

    /**
     * Get the last authenticated [Session], if there is an active session.
     * @return the last active session if any, or null
     */
    fun getLastAuthenticatedSession(): Session?

    /**
     * Create a session after a SSO successful login
     */
    fun createSessionFromSso(homeServerConnectionConfig: HomeServerConnectionConfig,
                             credentials: Credentials,
                             callback: MatrixCallback<Session>): Cancelable

    /**
     * Perform a wellknown request, using the domain from the matrixId
     */
    fun getWellKnownData(matrixId: String,
                         homeServerConnectionConfig: HomeServerConnectionConfig?,
                         callback: MatrixCallback<WellknownResult>): Cancelable

    /**
     * Authenticate with a matrixId and a password
     * Usually call this after a successful call to getWellKnownData()
     */
    fun directAuthentication(homeServerConnectionConfig: HomeServerConnectionConfig,
                             matrixId: String,
                             password: String,
                             initialDeviceName: String,
                             callback: MatrixCallback<Session>): Cancelable
}
