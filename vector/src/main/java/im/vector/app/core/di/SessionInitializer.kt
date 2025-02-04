/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
