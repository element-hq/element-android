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
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable

/**
 * This interface defines methods to authenticate to a matrix server.
 */
interface Authenticator {

    /**
     * @param homeServerConnectionConfig this param is used to configure the Homeserver
     * @param login the login field
     * @param password the password field
     * @param callback  the matrix callback on which you'll receive the result of authentication.
     * @return return a [Cancelable]
     */
    fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig, login: String, password: String, callback: MatrixCallback<Session>): Cancelable

    //TODO remove this method. Shouldn't be managed like that.
    /**
     * Check if there is an active [Session].
     * @return true if there is at least one active session.
     */
    fun hasActiveSessions(): Boolean

    //TODO remove this method. Shouldn't be managed like that.
    /**
     * Get the last active [Session], if there is an active session.
     * @return the lastActive session if any, or null
     */
    fun getLastActiveSession(): Session?


}