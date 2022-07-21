/*
 * Copyright 2022 New Vector Ltd
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

package im.vector.app.core.di

import android.content.Context
import im.vector.app.core.extensions.configureAndStart
import org.matrix.android.sdk.api.auth.AuthenticationService
import javax.inject.Inject

class ActiveSessionSetter @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val authenticationService: AuthenticationService,
        private val applicationContext: Context,
) {
    fun shouldSetActionSession(): Boolean {
        return authenticationService.hasAuthenticatedSessions() && !activeSessionHolder.hasActiveSession()
    }

    fun tryToSetActiveSession(startSync: Boolean) {
        if (shouldSetActionSession()) {
            val lastAuthenticatedSession = authenticationService.getLastAuthenticatedSession()!!
            activeSessionHolder.setActiveSession(lastAuthenticatedSession)
            lastAuthenticatedSession.configureAndStart(applicationContext, startSyncing = startSync)
        }
    }
}
