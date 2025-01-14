/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics

import im.vector.app.ActiveSessionDataSource
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.LiveEventListener
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.JsonDict
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// If we can decrypt in less than 4s, we don't report
private const val GRACE_PERIOD_MILLIS = 4_000

// A tick to check when a decryption failure as exceeded the max time
private const val CHECK_INTERVAL = 10_000L

// If we can't decrypt after 60s, we report failures
private const val MAX_WAIT_MILLIS = 60_000

/**
 * Tracks decryption errors.
 * When an error is reported it is not directly tracked via analytics, there is a grace period
 * that gives the app a few seconds to get the key to decrypt.
 *
 * Decrypted under 4s => No report
 * Decrypted before MAX_WAIT_MILLIS => Report with time to decrypt
 * Not Decrypted after MAX_WAIT_MILLIS => Report with time = -1
 */
@Singleton
class DecryptionFailureTracker @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val sessionDataSource: ActiveSessionDataSource,
        private val decryptionFailurePersistence: ReportedDecryptionFailurePersistence,
        private val clock: Clock
) : Session.Listener, LiveEventListener {

    // The active session (set by the sessionDataSource)
    private var activeSession: Session? = null

    // The coroutine scope to use for the tracker
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Map of eventId to tracked failure
    // Only accessed on a `post` call, ensuring sequential access
    private val trackedEventsMap = mutableMapOf<String, DecryptionFailure>()

    // Mutex to ensure sequential access to internal state
    private val mutex = Mutex()

    // Used to unsubscribe from the active session data source
    private lateinit var activeSessionSourceDisposable: Job

    // The ticker job, to report permanent UTD (not decrypted after MAX_WAIT_MILLIS)
    private var currentTicker: Job? = null

    /**
     * Start the tracker.
     *
     * @param scope The coroutine scope to use, exposed for tests. If null, it will use the default one
     */
    fun start(scope: CoroutineScope? = null) {
        if (scope != null) {
            this.scope = scope
        }
        observeActiveSession()
        post {
            decryptionFailurePersistence.load()
        }
    }

    fun stop() {
        Timber.v("Stop DecryptionFailureTracker")
        post {
                decryptionFailurePersistence.persist()
        }
        activeSessionSourceDisposable.cancel(CancellationException("Closing DecryptionFailureTracker"))

        activeSession?.removeListener(this)
        activeSession?.eventStreamService()?.removeEventStreamListener(this)
        activeSession = null
    }

    private fun post(block: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                block()
            }
        }
    }

    private suspend fun rescheduleTicker() {
        currentTicker = scope.launch {
            Timber.v("Reschedule ticker")
            delay(CHECK_INTERVAL)
            post {
                checkFailures()
                decryptionFailurePersistence.persist()
                currentTicker = null
                if (trackedEventsMap.isNotEmpty()) {
                    // Reschedule
                    rescheduleTicker()
                }
            }
        }
    }
    private fun observeActiveSession() {
       activeSessionSourceDisposable =  sessionDataSource.stream()
                .distinctUntilChanged()
                .onEach {
                    Timber.v("Active session changed ${it.getOrNull()?.myUserId}")
                    it.getOrNull()?.let { session ->
                        post {
                            onSessionActive(session)
                        }
                    }
                }.launchIn(scope)
    }

    private suspend fun onSessionActive(session: Session) {
        Timber.v("onSessionActive ${session.myUserId} previous: ${activeSession?.myUserId}")
        val sessionId = session.sessionId
        if (sessionId == activeSession?.sessionId) {
            return
        }
        this.activeSession?.let { previousSession ->
            previousSession.removeListener(this)
            previousSession.eventStreamService().removeEventStreamListener(this)
            // Do we want to clear the tracked events?
        }
        this.activeSession = session
        session.addListener(this)
        session.eventStreamService().addEventStreamListener(this)
    }

    override fun onSessionStopped(session: Session) {
        post {
            this.activeSession = null
            session.addListener(this)
            session.eventStreamService().addEventStreamListener(this)
        }
    }

    // LiveEventListener callbacks

    override fun onEventDecrypted(event: Event, clearEvent: JsonDict) {
        Timber.v("Event decrypted ${event.eventId}")
        event.eventId?.let {
            post {
                handleEventDecrypted(it)
            }
        }
    }

    override fun onEventDecryptionError(event: Event, cryptoError: MXCryptoError) {
        Timber.v("Decryption error for event ${event.eventId} with error $cryptoError")
        val session = activeSession ?: return
        // track the event
        post {
            trackEvent(session, event, cryptoError)
        }
    }

    override fun onLiveToDeviceEvent(event: Event) {}
    override fun onLiveEvent(roomId: String, event: Event) {}
    override fun onPaginatedEvent(roomId: String, event: Event) {}

    private suspend fun trackEvent(session: Session, event: Event, error: MXCryptoError) {
        Timber.v("Track event ${event.eventId}/${session.myUserId} time: ${clock.epochMillis()}")
        val eventId = event.eventId
        val roomId = event.roomId
        if (eventId == null || roomId == null) return
        if (trackedEventsMap.containsKey(eventId)) {
            // already tracked
            return
        }
        if (decryptionFailurePersistence.hasBeenReported(eventId)) {
            Timber.v("Event $eventId already reported")
            // already reported
            return
        }
        val isOwnIdentityTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified()
        val userHS = MatrixPatterns.extractServerNameFromId(session.myUserId)
        val messageSenderHs = event.senderId?.let {  MatrixPatterns.extractServerNameFromId(it) }
        Timber.v("senderHs: $messageSenderHs, userHS: $userHS, isOwnIdentityTrusted: $isOwnIdentityTrusted")

        val deviceCreationTs = session.cryptoService().getMyCryptoDevice().firstTimeSeenLocalTs
        Timber.v("deviceCreationTs: $deviceCreationTs")
        val eventRelativeAge = deviceCreationTs?.let { deviceTs ->
            event.originServerTs?.let {
                it - deviceTs
            }
        }
        val failure = DecryptionFailure(
                clock.epochMillis(),
                roomId,
                eventId,
                error,
                wasVisibleOnScreen = false,
                ownIdentityTrustedAtTimeOfDecryptionFailure = isOwnIdentityTrusted,
                isMatrixDotOrg = userHS == "matrix.org",
                isFederated = messageSenderHs?.let { it != userHS },
                eventLocalAgeAtDecryptionFailure = eventRelativeAge
        )
        Timber.v("Tracked failure: ${failure}")
        trackedEventsMap[eventId] = failure

        if (currentTicker == null) {
            rescheduleTicker()
        }
    }

    private suspend fun handleEventDecrypted(eventId: String) {
        Timber.v("Handle event decrypted $eventId time: ${clock.epochMillis()}")
        // Only consider if it was tracked as a failure
        val trackedFailure = trackedEventsMap[eventId] ?: return

        // Grace event if decrypted under 4s
        val now = clock.epochMillis()
        val timeToDecrypt = now - trackedFailure.timeStamp
        Timber.v("Handle event decrypted timeToDecrypt: $timeToDecrypt for event $eventId")
        if (timeToDecrypt < GRACE_PERIOD_MILLIS) {
            Timber.v("Grace event $eventId")
            trackedEventsMap.remove(eventId)
            return
        }
        // We still want to report but with the time it took
        if (trackedFailure.timeToDecryptMillis == null) {
            val decryptionFailure = trackedFailure.copy(timeToDecryptMillis = timeToDecrypt)
            trackedEventsMap[eventId] = decryptionFailure
            reportFailure(decryptionFailure)
        }
    }

     fun utdDisplayedInTimeline(event: TimelineEvent) {
        post {
            // should be tracked (unless already reported)
            val eventId = event.root.eventId ?: return@post
            val trackedEvent = trackedEventsMap[eventId] ?: return@post

            trackedEventsMap[eventId] = trackedEvent.copy(wasVisibleOnScreen = true)
        }
    }

    // This will mutate the trackedEventsMap, so don't call it while iterating on it.
    private suspend fun reportFailure(decryptionFailure: DecryptionFailure) {
        Timber.v("Report failure for event ${decryptionFailure.failedEventId}")
        val error = decryptionFailure.toAnalyticsEvent()

        analyticsTracker.capture(error)

        // now remove from tracked
        trackedEventsMap.remove(decryptionFailure.failedEventId)
        // mark as already reported
        decryptionFailurePersistence.markAsReported(decryptionFailure.failedEventId)
    }

    private suspend fun checkFailures() {
        val now = clock.epochMillis()
        Timber.v("Check failures now $now")
        // report the definitely failed
        val toReport = trackedEventsMap.values.filter {
            now - it.timeStamp > MAX_WAIT_MILLIS
        }
        toReport.forEach {
            reportFailure(
                    it.copy(timeToDecryptMillis = -1)
            )
        }
    }
}
