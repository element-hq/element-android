/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.network.token

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.RefreshResult
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError.Companion.M_FORBIDDEN
import org.matrix.android.sdk.api.failure.MatrixError.Companion.M_UNKNOWN_TOKEN
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.toFailure
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

internal class HomeserverAccessTokenProvider @Inject constructor(
        @SessionId private val sessionId: String,
        private val sessionParamsStore: SessionParamsStore,
        private val authenticationService: AuthenticationService,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AccessTokenProvider {

    companion object {
        private val mutex = Mutex()
    }

    object Constants {
        /**
        The time interval before the access token expires that we will start trying to refresh the token.
        This avoids us having to block other users requests while the token refreshes.
        Choosing a value larger than DEFAULT_DELAY_MILLIS + DEFAULT_LONG_POOL_TIMEOUT_SECONDS guarantees we will at least have attempted it before expiry.
         */
        const val PREEMPT_REFRESH_EXPIRATION_INTERVAL = 60000
    }

    override fun getToken(): String? {
        var accessToken: String?
        // We synchronise in a blocking fashion here so that when refresh is required, a single request becomes the leader.
        // On successful refresh the leader saves the credential and the new access token then becomes available to other requests when they are unblocked.
        // We should never send multiple refresh requests as refresh tokens are single-use(they rotate with a new one returned in the response).
        // Mishandled via race conditions and we could become unauthenticated.
        runBlocking {
            mutex.withLock {
                accessToken = verifyExpiryAndRefreshIfStale()
            }
        }
        return accessToken
    }

    private suspend fun verifyExpiryAndRefreshIfStale(): String? {
        val credentials = sessionParamsStore.get(sessionId)?.credentials ?: return null

        if (credentials.refreshToken.isNullOrEmpty() ||
                credentials.expiryTs == null ||
                System.currentTimeMillis() < (credentials.expiryTs - Constants.PREEMPT_REFRESH_EXPIRATION_INTERVAL)) {
            return credentials.accessToken
        }

        Timber.d("## Token Refresh: Refreshing access token...")

        var result: RefreshResult? = null
        try {
            result = authenticationService
                    .getRefreshWizard(sessionId)
                    .refresh(credentials.refreshToken)
        } catch (throwable: Throwable) {
            val serverError = throwable
                    .let { it as? HttpException }
                    ?.let { it.toFailure(globalErrorReceiver) as? Failure.ServerError }
            if (serverError != null) {
                Timber.d("## Token Refresh: Failed to refresh access token. error: $serverError")
                if (serverError.error.code == M_UNKNOWN_TOKEN || serverError.error.code == M_FORBIDDEN) {
                    Timber.d("## Token Refresh: Requires logout.")
                }
            }
        }

        if (result == null) {
            return null
        }

        val updatedCredentials = credentials.copy(
                accessToken = result.accessToken,
                expiresInMs = result.expiresInMs,
                expiryTs = System.currentTimeMillis() + result.expiresInMs,
                refreshToken = result.refreshToken
        )

        sessionParamsStore.updateCredentials(updatedCredentials)

        Timber.d("## Token Refresh: Tokens refreshed and saved.")

        return result.accessToken
    }
}
