/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.auth

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.LoginFlowResult
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.login.LoginWizard
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.auth.wellknown.WellknownResult
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable

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
     * Get an authenticated session. You should at least call authenticate one time before.
     * If you logout, this session will no longer be valid.
     *
     * @param sessionParams the sessionParams to open with.
     * @return the associated session if any, or null
     */
    fun getSession(sessionParams: SessionParams): Session?

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
