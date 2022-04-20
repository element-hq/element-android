/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app

import android.content.SharedPreferences
import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.ReportType
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val AUTO_RS_REQUEST = "im.vector.auto_rs_request"

@Singleton
class AutoRageShaker @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val activeSessionHolder: ActiveSessionHolder,
        private val bugReporter: BugReporter,
        private val vectorPreferences: VectorPreferences
) : Session.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val activeSessionIds = mutableSetOf<String>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentActiveSessionId: String? = null

    // Simple in memory cache of already sent report
    private data class ReportInfo(
            val roomId: String,
            val sessionId: String
    )

    private val alreadyReportedUisi = mutableListOf<ReportInfo>()

    private val e2eDetectedFlow = MutableSharedFlow<E2EMessageDetected>(replay = 0)
    private val matchingRSRequestFlow = MutableSharedFlow<Event>(replay = 0)
    private var hasSynced = false
    private var preferenceEnabled = false
    fun initialize() {
        observeActiveSession()
        preferenceEnabled = vectorPreferences.labsAutoReportUISI()
        // It's a singleton...
        vectorPreferences.subscribeToChanges(this)

        // Simple rate limit, notice that order is not
        // necessarily preserved
        e2eDetectedFlow
                .onEach {
                    sendRageShake(it)
                    delay(60_000)
                }
                .catch { cause ->
                    Timber.w(cause, "Failed to RS")
                }
                .launchIn(coroutineScope)

        matchingRSRequestFlow
                .onEach {
                    sendMatchingRageShake(it)
                    delay(60_000)
                }
                .catch { cause ->
                    Timber.w(cause, "Failed to send matching rageshake")
                }
                .launchIn(coroutineScope)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        preferenceEnabled = vectorPreferences.labsAutoReportUISI()
    }

    private fun observeActiveSession() {
        sessionDataSource.stream()
                .distinctUntilChanged()
                .onEach {
                    it.orNull()?.let { session ->
                        onSessionActive(session)
                    }
                }
                .launchIn(coroutineScope)
    }

    fun decryptionErrorDetected(target: E2EMessageDetected) {
        if (activeSessionHolder.getSafeActiveSession()?.sessionId != currentActiveSessionId) return
        val shouldSendRS = synchronized(alreadyReportedUisi) {
            val reportInfo = ReportInfo(target.roomId, target.sessionId)
            val alreadySent = alreadyReportedUisi.contains(reportInfo)
            if (!alreadySent) {
                alreadyReportedUisi.add(reportInfo)
            }
            !alreadySent
        }
        if (shouldSendRS) {
            coroutineScope.launch {
                e2eDetectedFlow.emit(target)
            }
        }
    }

    private fun sendRageShake(target: E2EMessageDetected) {
        bugReporter.sendBugReport(
                reportType = ReportType.AUTO_UISI,
                withDevicesLogs = true,
                withCrashLogs = true,
                withKeyRequestHistory = true,
                withScreenshot = false,
                theBugDescription = "Auto-reporting decryption error",
                serverVersion = "",
                canContact = false,
                customFields = mapOf("auto_uisi" to buildString {
                    append("{")
                    append("\"event_id\": \"${target.eventId}\",")
                    append("\"room_id\": \"${target.roomId}\",")
                    append("\"sender_key\": \"${target.senderKey}\",")
                    append("\"device_id\": \"${target.senderDeviceId}\",")
                    append("\"user_id\": \"${target.senderUserId}\",")
                    append("\"session_id\": \"${target.sessionId}\"")
                    append("}")
                }),
                listener = object : BugReporter.IMXBugReportListener {
                    override fun onUploadCancelled() {
                        synchronized(alreadyReportedUisi) {
                            alreadyReportedUisi.remove(ReportInfo(target.roomId, target.sessionId))
                        }
                    }

                    override fun onUploadFailed(reason: String?) {
                        synchronized(alreadyReportedUisi) {
                            alreadyReportedUisi.remove(ReportInfo(target.roomId, target.sessionId))
                        }
                    }

                    override fun onProgress(progress: Int) {
                    }

                    override fun onUploadSucceed(reportUrl: String?) {
                        // we need to send the toDevice message to the sender

                        coroutineScope.launch {
                            try {
                                activeSessionHolder.getSafeActiveSession()?.sendToDevice(
                                        eventType = AUTO_RS_REQUEST,
                                        userId = target.senderUserId,
                                        deviceId = target.senderDeviceId,
                                        content = mapOf(
                                                "event_id" to target.eventId,
                                                "room_id" to target.roomId,
                                                "session_id" to target.sessionId,
                                                "device_id" to target.senderDeviceId,
                                                "user_id" to target.senderUserId,
                                                "sender_key" to target.senderKey,
                                                "recipient_rageshake" to reportUrl
                                        ).toContent()
                                )
                            } catch (failure: Throwable) {
                                Timber.w("failed to send auto-uisi to device")
                            }
                        }
                    }
                })
    }

    fun remoteAutoUISIRequest(event: Event) {
        if (event.type != AUTO_RS_REQUEST) return
        if (activeSessionHolder.getSafeActiveSession()?.sessionId != currentActiveSessionId) return

        coroutineScope.launch {
            matchingRSRequestFlow.emit(event)
        }
    }

    private fun sendMatchingRageShake(event: Event) {
        val eventId = event.content?.get("event_id")
        val roomId = event.content?.get("room_id")
        val sessionId = event.content?.get("session_id")
        val deviceId = event.content?.get("device_id")
        val userId = event.content?.get("user_id")
        val senderKey = event.content?.get("sender_key")
        val matchingIssue = event.content?.get("recipient_rageshake")?.toString() ?: ""

        bugReporter.sendBugReport(
                reportType = ReportType.AUTO_UISI_SENDER,
                withDevicesLogs = true,
                withCrashLogs = true,
                withKeyRequestHistory = true,
                withScreenshot = false,
                theBugDescription = "Auto-reporting decryption error \nRecipient rageshake: $matchingIssue",
                serverVersion = "",
                canContact = false,
                customFields = mapOf(
                        "auto_uisi" to buildString {
                            append("{")
                            append("\"event_id\": \"$eventId\",")
                            append("\"room_id\": \"$roomId\",")
                            append("\"sender_key\": \"$senderKey\",")
                            append("\"device_id\": \"$deviceId\",")
                            append("\"user_id\": \"$userId\",")
                            append("\"session_id\": \"$sessionId\"")
                            append("}")
                        },
                        "recipient_rageshake" to matchingIssue
                ),
                listener = null
        )
    }

    private val detector = UISIDetector().apply {
        callback = object : UISIDetector.UISIDetectorCallback {
            override val reciprocateToDeviceEventType: String
                get() = AUTO_RS_REQUEST

            override val enabled: Boolean
                get() = this@AutoRageShaker.preferenceEnabled && this@AutoRageShaker.hasSynced

            override fun uisiDetected(source: E2EMessageDetected) {
                decryptionErrorDetected(source)
            }

            override fun uisiReciprocateRequest(source: Event) {
                remoteAutoUISIRequest(source)
            }
        }
    }

    fun onSessionActive(session: Session) {
        val sessionId = session.sessionId
        if (sessionId == currentActiveSessionId) {
            return
        }
        this.currentActiveSessionId = sessionId

        hasSynced = session.hasAlreadySynced()
        session.getSyncStatusLive()
                .asFlow()
                .onEach {
                    hasSynced = it !is SyncStatusService.Status.InitialSyncProgressing
                }
                .launchIn(session.coroutineScope)
        activeSessionIds.add(sessionId)
        session.addListener(this)
        session.addEventStreamListener(detector)
    }

    override fun onSessionStopped(session: Session) {
        session.removeEventStreamListener(detector)
        activeSessionIds.remove(session.sessionId)
    }
}
