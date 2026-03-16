/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.appfunctions

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Build
import android.os.ResultReceiver
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.translation.TranslationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import timber.log.Timber
import javax.inject.Inject

/**
 * ElementAppFunctionService — exposes Element Android messaging capabilities
 * for inter-app orchestration by AI agents (Off Grid Mobile AI, etc.).
 *
 * Supports both Android 16 AppFunctions (future) and Intent-based API (current).
 *
 * Intent Actions:
 *  - im.vector.app.ACTION_SEARCH_MESSAGES  (extras: query, maxResults)
 *  - im.vector.app.ACTION_READ_MESSAGES    (extras: roomId, count)
 *  - im.vector.app.ACTION_SEND_MESSAGE     (extras: roomId, message)
 *  - im.vector.app.ACTION_LIST_ROOMS       (no extras)
 *  - im.vector.app.ACTION_SUMMARIZE_ROOM   (extras: roomId, messageCount)
 *  - im.vector.app.ACTION_UNREAD_SUMMARY   (no extras)
 *
 * All results are returned as JSON strings in the "result" extra of the result Bundle
 * via the ResultReceiver provided in the "resultReceiver" extra.
 */
@AndroidEntryPoint
class ElementAppFunctionService : Service() {

    companion object {
        private const val TAG = "APPFUNCTIONS"

        const val ACTION_SEARCH_MESSAGES = "im.vector.app.ACTION_SEARCH_MESSAGES"
        const val ACTION_READ_MESSAGES = "im.vector.app.ACTION_READ_MESSAGES"
        const val ACTION_SEND_MESSAGE = "im.vector.app.ACTION_SEND_MESSAGE"
        const val ACTION_LIST_ROOMS = "im.vector.app.ACTION_LIST_ROOMS"
        const val ACTION_SUMMARIZE_ROOM = "im.vector.app.ACTION_SUMMARIZE_ROOM"
        const val ACTION_UNREAD_SUMMARY = "im.vector.app.ACTION_UNREAD_SUMMARY"

        const val EXTRA_QUERY = "query"
        const val EXTRA_MAX_RESULTS = "maxResults"
        const val EXTRA_ROOM_ID = "roomId"
        const val EXTRA_COUNT = "count"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_MESSAGE_COUNT = "messageCount"
        const val EXTRA_RESULT_RECEIVER = "resultReceiver"

        const val RESULT_CODE_SUCCESS = 0
        const val RESULT_CODE_ERROR = 1
        const val RESULT_KEY = "result"
        const val ERROR_KEY = "error"
    }

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var translationService: TranslationService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    override fun onBind(intent: Intent?): IBinder? {
        // No binding for now; use startService with ResultReceiver
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("onStartCommand action=${intent?.action}")
        val resultReceiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_RECEIVER, ResultReceiver::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_RECEIVER)
        }

        if (intent == null) {
            sendError(resultReceiver, "No intent provided")
            stopSelfIfIdle(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                val session = activeSessionHolder.getSafeActiveSession()
                if (session == null) {
                    sendError(resultReceiver, "No active session. User is not logged in.")
                    stopSelfIfIdle(startId)
                    return@launch
                }

                val result = when (intent.action) {
                    ACTION_SEARCH_MESSAGES -> {
                        val query = intent.getStringExtra(EXTRA_QUERY) ?: ""
                        val maxResults = intent.getIntExtra(EXTRA_MAX_RESULTS, 10)
                        searchMessages(session, query, maxResults)
                    }
                    ACTION_READ_MESSAGES -> {
                        val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: ""
                        val count = intent.getIntExtra(EXTRA_COUNT, 20)
                        readMessages(session, roomId, count)
                    }
                    ACTION_SEND_MESSAGE -> {
                        val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: ""
                        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                        sendMessage(session, roomId, message)
                    }
                    ACTION_LIST_ROOMS -> {
                        listRooms(session)
                    }
                    ACTION_SUMMARIZE_ROOM -> {
                        val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: ""
                        val messageCount = intent.getIntExtra(EXTRA_MESSAGE_COUNT, 50)
                        summarizeRoom(session, roomId, messageCount)
                    }
                    ACTION_UNREAD_SUMMARY -> {
                        getUnreadSummary(session)
                    }
                    else -> {
                        """{"error":"Unknown action: ${intent.action}"}"""
                    }
                }

                sendSuccess(resultReceiver, result)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing action ${intent.action}")
                sendError(resultReceiver, e.message ?: "Unknown error")
            } finally {
                stopSelfIfIdle(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ─── Search Messages ──────────────────────────────────────────────

    private suspend fun searchMessages(session: Session, query: String, maxResults: Int): String {
        if (query.isBlank()) return "[]"

        Timber.tag(TAG).d("searchMessages query='$query' maxResults=$maxResults")

        val results = mutableListOf<MessageResult>()

        // Search across all joined rooms
        val roomSummaries = session.roomService().getRoomSummaries(
                roomSummaryQueryParams {
                    memberships = listOf(Membership.JOIN)
                }
        )

        for (summary in roomSummaries) {
            if (results.size >= maxResults) break
            try {
                val searchResult = withContext(Dispatchers.IO) {
                    session.searchService().search(
                            searchTerm = query,
                            roomId = summary.roomId,
                            nextBatch = null,
                            orderByRecent = true,
                            limit = maxResults - results.size,
                            beforeLimit = 0,
                            afterLimit = 0,
                            includeProfile = true
                    )
                }
                searchResult.results?.forEach { eventAndSender ->
                    val event = eventAndSender.event
                    val body = event.getClearContent()?.toModel<MessageContent>()?.body ?: return@forEach
                    results.add(MessageResult(
                            roomId = summary.roomId,
                            roomName = summary.displayName,
                            senderId = event.senderId ?: "",
                            senderName = eventAndSender.sender?.displayName ?: event.senderId ?: "",
                            body = body,
                            timestamp = event.originServerTs ?: 0L,
                            eventId = event.eventId ?: ""
                    ))
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Search failed in room ${summary.roomId}")
                // Continue to next room
            }
        }

        val adapter = moshi.adapter<List<MessageResult>>(
                Types.newParameterizedType(List::class.java, MessageResult::class.java)
        )
        return adapter.toJson(results.take(maxResults))
    }

    // ─── Read Messages ────────────────────────────────────────────────

    private suspend fun readMessages(session: Session, roomId: String, count: Int): String {
        if (roomId.isBlank()) return """{"error":"roomId is required"}"""

        Timber.tag(TAG).d("readMessages roomId='$roomId' count=$count")

        val room = session.getRoom(roomId) ?: return """{"error":"Room not found: $roomId"}"""
        val summary = session.getRoomSummary(roomId)

        val results = mutableListOf<MessageResult>()

        // Create a timeline, get snapshot, dispose
        val timeline = room.timelineService().createTimeline(
                eventId = null,
                settings = TimelineSettings(initialSize = count, buildReadReceipts = false)
        )
        try {
            withContext(Dispatchers.IO) {
                timeline.start()
                // Poll until the timeline snapshot is non-empty or we hit the max wait time.
                // The timeline needs time to load events from the local database after start().
                val maxWaitMs = 3000L
                val pollIntervalMs = 200L
                var waited = 0L
                while (waited < maxWaitMs) {
                    if (timeline.getSnapshot().isNotEmpty()) break
                    kotlinx.coroutines.delay(pollIntervalMs)
                    waited += pollIntervalMs
                }
            }

            val snapshot = timeline.getSnapshot()

            for (event in snapshot) {
                val clearType = event.root.getClearType()
                if (clearType != EventType.MESSAGE) continue

                val content = event.getLastMessageContent() ?: continue
                val body = content.body

                results.add(MessageResult(
                        roomId = roomId,
                        roomName = summary?.displayName ?: roomId,
                        senderId = event.senderInfo.userId,
                        senderName = event.senderInfo.disambiguatedDisplayName,
                        body = body,
                        timestamp = event.root.originServerTs ?: 0L,
                        eventId = event.eventId
                ))

                if (results.size >= count) break
            }
        } finally {
            timeline.dispose()
        }

        val adapter = moshi.adapter<List<MessageResult>>(
                Types.newParameterizedType(List::class.java, MessageResult::class.java)
        )
        return adapter.toJson(results)
    }

    // ─── Send Message ─────────────────────────────────────────────────

    private suspend fun sendMessage(session: Session, roomId: String, message: String): String {
        if (roomId.isBlank()) return """{"success":false,"error":"roomId is required"}"""
        if (message.isBlank()) return """{"success":false,"error":"message is required"}"""

        Timber.tag(TAG).d("sendMessage roomId='$roomId' message length=${message.length}")

        val room = session.getRoom(roomId) ?: return """{"success":false,"error":"Room not found: $roomId"}"""

        return try {
            room.sendService().sendTextMessage(message)
            """{"success":true}"""
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to send message to $roomId")
            """{"success":false,"error":"${e.message?.replace("\"", "\\\"") ?: "Unknown error"}"}"""
        }
    }

    // ─── List Rooms ───────────────────────────────────────────────────

    private fun listRooms(session: Session): String {
        Timber.tag(TAG).d("listRooms")

        val summaries = session.roomService().getRoomSummaries(
                roomSummaryQueryParams {
                    memberships = listOf(Membership.JOIN)
                }
        )

        val rooms = summaries.map { summary ->
            val lastEvent = summary.latestPreviewableEvent
            val lastBody = lastEvent?.getLastMessageContent()?.body
            val lastSender = lastEvent?.senderInfo?.disambiguatedDisplayName

            RoomInfo(
                    roomId = summary.roomId,
                    displayName = summary.displayName,
                    lastMessage = lastBody,
                    lastMessageSender = lastSender,
                    unreadCount = summary.notificationCount,
                    isDirect = summary.isDirect
            )
        }

        val adapter = moshi.adapter<List<RoomInfo>>(
                Types.newParameterizedType(List::class.java, RoomInfo::class.java)
        )
        return adapter.toJson(rooms)
    }

    // ─── Summarize Room ───────────────────────────────────────────────

    private suspend fun summarizeRoom(session: Session, roomId: String, messageCount: Int): String {
        if (roomId.isBlank()) return """{"error":"roomId is required"}"""

        Timber.tag(TAG).d("summarizeRoom roomId='$roomId' messageCount=$messageCount")

        // First, read the messages
        val messagesJson = readMessages(session, roomId, messageCount)
        val adapter = moshi.adapter<List<MessageResult>>(
                Types.newParameterizedType(List::class.java, MessageResult::class.java)
        )
        val messages = adapter.fromJson(messagesJson) ?: emptyList()

        if (messages.isEmpty()) {
            return """{"summary":"No messages found in this room."}"""
        }

        // Build conversation text for the AI
        val conversation = messages.reversed().joinToString("\n") { msg ->
            "[${msg.senderName}]: ${msg.body}"
        }

        val summary = translationService.complete(
                systemPrompt = "You are a helpful assistant. Summarize the following conversation concisely. " +
                        "Focus on the key topics discussed, decisions made, and action items. " +
                        "Reply in the same language as the majority of the messages. " +
                        "Keep the summary under 200 words.",
                userMessage = conversation
        )

        return if (summary != null) {
            val resultMap = mapOf("summary" to summary, "messageCount" to messages.size)
            moshi.adapter<Map<String, Any>>(
                    Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).toJson(resultMap)
        } else {
            """{"error":"AI summarization failed. Check TranslationService configuration.","messageCount":${messages.size}}"""
        }
    }

    // ─── Unread Summary ───────────────────────────────────────────────

    private fun getUnreadSummary(session: Session): String {
        Timber.tag(TAG).d("getUnreadSummary")

        val summaries = session.roomService().getRoomSummaries(
                roomSummaryQueryParams {
                    memberships = listOf(Membership.JOIN)
                }
        )

        val roomUnreads = summaries
                .filter { it.notificationCount > 0 || it.highlightCount > 0 }
                .map { summary ->
                    RoomUnread(
                            roomId = summary.roomId,
                            roomName = summary.displayName,
                            unreadCount = summary.notificationCount,
                            hasMentions = summary.highlightCount > 0
                    )
                }

        val unreadSummary = UnreadSummary(
                totalUnread = roomUnreads.sumOf { it.unreadCount },
                totalMentions = summaries.sumOf { it.highlightCount },
                rooms = roomUnreads
        )

        return moshi.adapter(UnreadSummary::class.java).toJson(unreadSummary)
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private fun sendSuccess(receiver: ResultReceiver?, result: String) {
        receiver?.send(RESULT_CODE_SUCCESS, Bundle().apply {
            putString(RESULT_KEY, result)
        })
    }

    private fun sendError(receiver: ResultReceiver?, error: String) {
        Timber.tag(TAG).e("Error: $error")
        receiver?.send(RESULT_CODE_ERROR, Bundle().apply {
            putString(ERROR_KEY, error)
        })
    }

    private fun stopSelfIfIdle(startId: Int) {
        stopSelf(startId)
    }
}
