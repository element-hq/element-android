/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.di.MatrixComponent
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.android.internal.session.DaggerSessionComponent
import im.vector.matrix.android.internal.session.SessionComponent
import javax.inject.Inject

@MatrixScope
internal class SessionManager @Inject constructor(private val matrixComponent: MatrixComponent) {

    private val sessionComponents = HashMap<String, SessionComponent>()

    fun getSessionComponent(userId: String): SessionComponent? {
        return sessionComponents[userId]
    }

    fun createSession(sessionParams: SessionParams): Session {
        val userId = sessionParams.credentials.userId
        if (sessionComponents.containsKey(userId)) {
            throw RuntimeException("You already have a session for the user $userId")
        }
        return DaggerSessionComponent
                .factory()
                .create(matrixComponent, sessionParams)
                .also {
                    sessionComponents[userId] = it
                }.let {
                    it.session()
                }
    }

    fun releaseSession(userId: String) {
        if (sessionComponents.containsKey(userId).not()) {
            throw RuntimeException("You don't have a session for the user $userId")
        }
        sessionComponents.remove(userId)?.also {
            it.session().close()
        }
    }


}