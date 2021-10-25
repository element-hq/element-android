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

import android.content.Context
import android.content.SharedPreferences
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.ReportType
import im.vector.app.features.settings.VectorPreferences
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val AUTO_RS_REQUEST = "im.vector.auto_rs_request"

@Singleton
class AutoRageShaker @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val activeSessionHolder: ActiveSessionHolder,
        private val bugReporter: BugReporter,
        private val context: Context,
        private val vectorPreferences: VectorPreferences
) : Session.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var activeSessionDisposable: Disposable
    private val activeSessionIds = mutableSetOf<String>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val uisiDetectors = mutableMapOf<String, UISIDetector>()
    private var currentActiveSessionId: String? = null

    fun initialize() {
        observeActiveSession()
        // It's a singleton...
        vectorPreferences.subscribeToChanges(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        enable(vectorPreferences.labsAutoReportUISI())
    }

    var _enabled = false
    fun enable(enabled: Boolean) {
        if (enabled == _enabled) return
        _enabled = enabled
        uisiDetectors.forEach { it.value.enabled = enabled }
    }

    private fun observeActiveSession() {
        activeSessionDisposable = sessionDataSource.observe()
                .distinctUntilChanged()
                .subscribe {
                    it.orNull()?.let { session ->
                        onSessionActive(session)
                    }
                }
    }

    fun decryptionErrorDetected(target: E2EMessageDetected) {
        if (target.source == UISIEventSource.INITIAL_SYNC) return
        if (activeSessionHolder.getSafeActiveSession()?.sessionId != currentActiveSessionId) return
        coroutineScope.launch {
            bugReporter.sendBugReport(
                    context = context,
                    reportType = ReportType.AUTO_UISI,
                    withDevicesLogs = true,
                    withCrashLogs = true,
                    withKeyRequestHistory = true,
                    withScreenshot = false,
                    theBugDescription = "UISI detected",
                    serverVersion = "",
                    canContact = false,
                    customFields = mapOf("auto-uisi" to buildString {
                        append("\neventId: ${target.eventId}")
                        append("\nroomId: ${target.roomId}")
                        append("\nsenderKey: ${target.senderKey}")
                        append("\nsource: ${target.source}")
                        append("\ndeviceId: ${target.senderDeviceId}")
                        append("\nuserId: ${target.senderUserId}")
                        append("\nsessionId: ${target.sessionId}")
                    }),
                    listener = object : BugReporter.IMXBugReportListener {
                        override fun onUploadCancelled() {
                        }

                        override fun onUploadFailed(reason: String?) {
                        }

                        override fun onProgress(progress: Int) {
                        }

                        override fun onUploadSucceed(reportUrl: String?) {
                            Timber.w("## VALR Report URL is $reportUrl")
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
                                                    "matching_issue" to reportUrl
                                            ).toContent()
                                    )
                                } catch (failure: Throwable) {
                                    Timber.w("## VALR : failed to send auto-uisi to device")
                                }
                            }
                        }
                    })
        }
    }

    fun remoteAutoUISIRequest(event: Event) {
        if (event.type != AUTO_RS_REQUEST) return
        if (activeSessionHolder.getSafeActiveSession()?.sessionId != currentActiveSessionId) return

        coroutineScope.launch {
            val eventId = event.content?.get("event_id")
            val roomId = event.content?.get("room_id")
            val sessionId = event.content?.get("session_id")
            val deviceId = event.content?.get("device_id")
            val userId = event.content?.get("user_id")
            val senderKey = event.content?.get("sender_key")
            val matchingIssue = event.content?.get("matching_issue")?.toString() ?: ""

            bugReporter.sendBugReport(
                    context = context,
                    reportType = ReportType.AUTO_UISI_SENDER,
                    withDevicesLogs = true,
                    withCrashLogs = true,
                    withKeyRequestHistory = true,
                    withScreenshot = false,
                    theBugDescription = "UISI detected $matchingIssue",
                    serverVersion = "",
                    canContact = false,
                    customFields = mapOf<String, String>(
                            "auto-uisi" to buildString {
                                append("\neventId: $eventId")
                                append("\nroomId: $roomId")
                                append("\nsenderKey: $senderKey")
                                append("\ndeviceId: $deviceId")
                                append("\nuserId: $userId")
                                append("\nsessionId: $sessionId")
                            },
                            "matching_issue" to matchingIssue
                    ),
                    listener = null
            )
        }
    }

    private val detector = UISIDetector().apply {
        callback = object : UISIDetector.UISIDetectorCallback {
            override val reciprocateToDeviceEventType: String
                get() = AUTO_RS_REQUEST

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
        this.detector.enabled = _enabled
        activeSessionIds.add(sessionId)
        session.addListener(this)
        session.addEventStreamListener(detector)
    }

    override fun onSessionStopped(session: Session) {
        uisiDetectors.get(session.sessionId)?.let {
            session.removeEventStreamListener(it)
        }
        activeSessionIds.remove(session.sessionId)
    }
}
