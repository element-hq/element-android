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

package im.vector.riotredesign.core.di

import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.sync.FilterService
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionHolder @Inject constructor(private val authenticator: Authenticator) {

    private var activeSession: AtomicReference<Session?> = AtomicReference()

    fun setActiveSession(session: Session){
        activeSession.set(session)
    }

    fun clearActiveSession(){
        activeSession.set(null)
    }

    fun hasActiveSession(): Boolean {
        return activeSession.get() != null
    }

    fun getActiveSession(): Session {
        return activeSession.get() ?: throw IllegalStateException("You should authenticate before using this")
    }

    //TODO: Stop sync ?
    fun switchToSession(sessionParams: SessionParams) {
        val newActiveSession = authenticator.getSession(sessionParams)
        activeSession.set(newActiveSession)
    }

}