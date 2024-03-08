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

package im.vector.app.features.analytics

import im.vector.app.features.analytics.plan.Error
import im.vector.lib.core.utils.compat.removeIfCompat
import im.vector.lib.core.utils.flow.tickerFlow
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject
import javax.inject.Singleton

private data class DecryptionFailure(
        val timeStamp: Long,
        val roomId: String,
        val failedEventId: String,
        val error: MXCryptoError.ErrorType,
        // Was the current session cross signed verified at the time of the error
        val isCrossSignedVerified: Boolean = false
)
private typealias DetailedErrorName = Pair<String, Error.Name>

private const val GRACE_PERIOD_MILLIS = 4_000
private const val CHECK_INTERVAL = 2_000L

/**
 * Tracks decryption errors that are visible to the user.
 * When an error is reported it is not directly tracked via analytics, there is a grace period
 * that gives the app a few seconds to get the key to decrypt.
 */
@Singleton
class DecryptionFailureTracker @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val clock: Clock
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
    private val failures = mutableListOf<DecryptionFailure>()
    private val alreadyReported = mutableListOf<String>()

    var currentModule: Error.CryptoModule? = null

    init {
        start()
    }

    fun start() {
        tickerFlow(scope, CHECK_INTERVAL)
                .onEach {
                    checkFailures()
                }.launchIn(scope)
    }

    fun stop() {
        scope.cancel()
    }

    fun e2eEventDisplayedInTimeline(event: TimelineEvent, session: Session) {
        scope.launch(Dispatchers.Default) {
            val mCryptoError = event.root.mCryptoError
            if (mCryptoError != null) {
                val isVerified = session.cryptoService().crossSigningService().isCrossSigningVerified()
                addDecryptionFailure(
                        DecryptionFailure(clock.epochMillis(), event.roomId, event.eventId, mCryptoError, isVerified)
                )
            } else {
                removeFailureForEventId(event.eventId)
            }
        }
    }

    /**
     * Can be called when the timeline is disposed in order
     * to grace those events as they are not anymore displayed on screen.
     * */
    fun onTimeLineDisposed(roomId: String) {
        scope.launch(Dispatchers.Default) {
            synchronized(failures) {
                failures.removeIfCompat { it.roomId == roomId }
            }
        }
    }

    private fun addDecryptionFailure(failure: DecryptionFailure) {
        // de duplicate
        synchronized(failures) {
            if (failures.none { it.failedEventId == failure.failedEventId }) {
                failures.add(failure)
            }
        }
    }

    private fun removeFailureForEventId(eventId: String) {
        synchronized(failures) {
            failures.removeIfCompat { it.failedEventId == eventId }
        }
    }

    private fun checkFailures() {
        val now = clock.epochMillis()
        val aggregatedErrors: Map<DetailedErrorName, List<DecryptionFailure>>
        synchronized(failures) {
            val toReport = mutableListOf<DecryptionFailure>()
            failures.removeAll { failure ->
                (now - failure.timeStamp > GRACE_PERIOD_MILLIS).also {
                    if (it) {
                        toReport.add(failure)
                    }
                }
            }

            aggregatedErrors = toReport
                    .groupBy { it.error.toAnalyticsErrorName() }
                    .mapValues {
                        it.value
                    }
        }

        aggregatedErrors.forEach { aggregation ->
            // there is now way to send the total/sum in posthog, so iterating
            aggregation.value
                    // for now we ignore events already reported even if displayed again?
                    .filter { alreadyReported.contains(it.failedEventId).not() }
                    .forEach { failure ->
                        analyticsTracker.capture(
                                Error(
                                        context = aggregation.key.first,
                                        domain = Error.Domain.E2EE,
                                        name = aggregation.key.second,
                                        cryptoModule = currentModule,
                                ), mapOf("is_cross_signed_verified" to failure.isCrossSignedVerified.toString())
                        )
                        alreadyReported.add(failure.failedEventId)
                    }
        }
    }

    private fun MXCryptoError.ErrorType.toAnalyticsErrorName(): DetailedErrorName {
        val detailed = "$name | mxc_crypto_error_type"
        val errorName = when (this) {
            MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID,
            MXCryptoError.ErrorType.KEYS_WITHHELD -> Error.Name.OlmKeysNotSentError
            MXCryptoError.ErrorType.OLM -> Error.Name.OlmUnspecifiedError
            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX -> Error.Name.OlmIndexError
            else -> Error.Name.UnknownError
        }
        return DetailedErrorName(detailed, errorName)
    }
}
