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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.auth.data.RefreshResult
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.failure.MatrixError.Companion.M_FORBIDDEN
import org.matrix.android.sdk.api.failure.MatrixError.Companion.M_UNKNOWN_TOKEN
import org.matrix.android.sdk.api.failure.isTokenUnknownError
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.auth.refresh.RefreshTokenTask
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import timber.log.Timber
import javax.inject.Inject

internal class HomeserverAccessTokenProvider @Inject constructor(
        @SessionId private val sessionId: String,
        private val sessionParamsStore: SessionParamsStore,
        private val refreshTokenTask: RefreshTokenTask,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AccessTokenProvider {

    companion object {
        private val mutex = Mutex()
        
        /**
        The time interval before the access token expires that we will start trying to refresh the token.
        This avoids us having to block other users requests while the token refreshes.
        Choosing a value larger than DEFAULT_DELAY_MILLIS + DEFAULT_LONG_POOL_TIMEOUT_SECONDS guarantees we will at least have attempted it before expiry.
         */
        private const val PREEMPT_REFRESH_EXPIRATION_INTERVAL = 60000
    }

    override suspend fun getToken(serverError: Failure.ServerError?): String? {
        var accessToken: String?
        // We synchronise here so that when refresh is required, a single request becomes the leader.
        // On successful refresh the leader saves the credential and the new access token then becomes available to other requests when they are unblocked.
        // We should never send multiple refresh requests as refresh tokens are single-use(they rotate with a new one returned in the response).
        // Mishandled via race conditions and we could become unauthenticated.
        mutex.withLock {
            accessToken = verifyAccessTokenAndRefreshIfStale(serverError)
        }
        return accessToken
    }

    private suspend fun verifyAccessTokenAndRefreshIfStale(serverError: Failure.ServerError?): String? {
        val credentials = sessionParamsStore.get(sessionId)?.credentials ?: return null
        val receivedTokenUnknown = serverError?.isTokenUnknownError().orFalse()

        if (credentials.refreshToken.isNullOrEmpty() && serverError != null &&  receivedTokenUnknown) {
            Timber.d("## HomeserverAccessTokenProvider: accessToken-based auth failed, requires logout.")
            globalErrorReceiver.handleGlobalError(invalidToken(serverError, false))
        }

        if (credentials.refreshToken.isNullOrEmpty() || (!receivedTokenUnknown && expiryIsValid(credentials.expiryTs))) {
            // Existing access token is valid
            return credentials.accessToken
        }

        Timber.d("## HomeserverAccessTokenProvider: Refreshing access token...")

        var result: RefreshResult? = null
        try {
            result = refreshTokenTask.execute(RefreshTokenTask.Params(credentials.refreshToken))
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError) {
                Timber.d("## HomeserverAccessTokenProvider: Failed to refresh access token. error: $throwable")
                if (throwable.error.code == M_UNKNOWN_TOKEN || throwable.error.code == M_FORBIDDEN) {
                    Timber.d("## HomeserverAccessTokenProvider: refreshToken-based auth failed, requires logout.")
                    globalErrorReceiver.handleGlobalError(invalidToken(throwable, true))
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
                refreshToken = result.refreshToken + "fake"
        )

        sessionParamsStore.updateCredentials(updatedCredentials)

        Timber.d("## HomeserverAccessTokenProvider: Tokens refreshed and saved.")

        return result.accessToken
    }

    private fun expiryIsValid(expiryTs: Long?) = expiryTs == null || System.currentTimeMillis() < (expiryTs - PREEMPT_REFRESH_EXPIRATION_INTERVAL)

    private fun invalidToken(serverError: Failure.ServerError, refreshTokenAuth: Boolean) = GlobalError.InvalidToken(
            softLogout = serverError.error.isSoftLogout.orFalse(),
            refreshTokenAuth = refreshTokenAuth,
            errorCode = serverError.error.code,
            errorReason = serverError.error.message
    )
}
