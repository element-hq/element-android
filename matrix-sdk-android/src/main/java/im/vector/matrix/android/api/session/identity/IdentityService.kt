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

    fun setNewIdentityServer(url: String?, callback: MatrixCallback<Unit>): Cancelable

    fun disconnect()

    fun startBindSession(threePid: ThreePid, nothing: Nothing?, matrixCallback: MatrixCallback<ThreePid>)
    fun finalizeBindSessionFor3PID(threePid: ThreePid, matrixCallback: MatrixCallback<Unit>)
    fun submitValidationToken(pid: ThreePid, code: String, matrixCallback: MatrixCallback<Unit>)

    fun startUnBindSession(threePid: ThreePid, nothing: Nothing?, matrixCallback: MatrixCallback<Pair<Boolean, ThreePid?>>)

    fun lookUp(threePids: List<ThreePid>, callback: MatrixCallback<List<FoundThreePid>>): Cancelable

    fun addListener(listener: IdentityServiceListener)
    fun removeListener(listener: IdentityServiceListener)
}
