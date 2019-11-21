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
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.login.LoginWizard
import im.vector.matrix.android.api.auth.registration.RegistrationWizard
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse

/**
 * This interface defines methods to authenticate or to create an account to a matrix server.
 */
interface AuthenticationService {

    /**
     * Request the supported login flows for this homeserver
     */
    fun getLoginFlow(homeServerConnectionConfig: HomeServerConnectionConfig, callback: MatrixCallback<LoginFlowResponse>): Cancelable

    /**
     * Return an LoginWizard
     * @param homeServerConnectionConfig this param is used to request the Homeserver
     */
    fun createLoginWizard(homeServerConnectionConfig: HomeServerConnectionConfig): LoginWizard

    /**
     * Return a RegistrationWizard, to create an matrix account on a homeserver
     */
    fun getOrCreateRegistrationWizard(homeServerConnectionConfig: HomeServerConnectionConfig): RegistrationWizard

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
}
