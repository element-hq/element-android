/*
 * Copyright (c) 2022 New Vector Ltd
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

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

private val initializerSemaphore = Semaphore(permits = 1)

class SessionInitializer @Inject constructor(
        private val authenticationService: AuthenticationService,
) {

    /**
     * A thread safe way to initialize the last authenticated Session instance.
     *
     * @param readCurrentSession expects an in-memory Session to be provided or null if not yet set.
     * @param initializer callback to allow additional initialization on the Session, such as setting the in-memory Session instance.
     * @return the initialized Session or null when no authenticated sessions are available.
     */
    suspend fun tryInitialize(readCurrentSession: () -> Session?, initializer: (Session) -> Unit): Session? {
        return initializerSemaphore.withPermit {
            val currentInMemorySession = readCurrentSession()
            when {
                currentInMemorySession != null -> currentInMemorySession
                authenticationService.hasAuthenticatedSessions() -> {
                    val lastAuthenticatedSession = authenticationService.getLastAuthenticatedSession()!!
                    lastAuthenticatedSession.also { initializer(lastAuthenticatedSession) }
                }
                else -> null
            }
        }
    }
}
