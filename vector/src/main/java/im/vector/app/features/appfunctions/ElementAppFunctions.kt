/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.appfunctions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Element Android AppFunctions — high-level API for inter-app AI orchestration.
 *
 * This class provides a convenient Kotlin API that wraps the Intent-based
 * ElementAppFunctionService. It can be used directly within the app, or
 * will be annotated with @AppFunction once the Jetpack library is stable.
 *
 * Functions exposed:
 * - searchMessages: Search messages across all rooms
 * - readMessages: Read the last N messages from a room
 * - sendMessage: Send a text message to a room
 * - listRooms: List all joined rooms
 * - summarizeRoom: Summarize a room conversation using AI
 * - getUnreadSummary: Get unread counts across all rooms
 */
class ElementAppFunctions(private val context: Context) {

    companion object {
        private const val TAG = "APPFUNCTIONS"
        private const val SERVICE_TIMEOUT_MS = 30_000L
    }

    /** Search messages across all rooms matching the given query. */
    suspend fun searchMessages(query: String, maxResults: Int = 10): String {
        return callService(ElementAppFunctionService.ACTION_SEARCH_MESSAGES) {
            putExtra(ElementAppFunctionService.EXTRA_QUERY, query)
            putExtra(ElementAppFunctionService.EXTRA_MAX_RESULTS, maxResults)
        }
    }

    /** Read the last N messages from a specific room. */
    suspend fun readMessages(roomId: String, count: Int = 20): String {
        return callService(ElementAppFunctionService.ACTION_READ_MESSAGES) {
            putExtra(ElementAppFunctionService.EXTRA_ROOM_ID, roomId)
            putExtra(ElementAppFunctionService.EXTRA_COUNT, count)
        }
    }

    /** Send a text message to a specific room. */
    suspend fun sendMessage(roomId: String, message: String): String {
        return callService(ElementAppFunctionService.ACTION_SEND_MESSAGE) {
            putExtra(ElementAppFunctionService.EXTRA_ROOM_ID, roomId)
            putExtra(ElementAppFunctionService.EXTRA_MESSAGE, message)
        }
    }

    /** List all joined rooms with their display name, last message, and unread count. */
    suspend fun listRooms(): String {
        return callService(ElementAppFunctionService.ACTION_LIST_ROOMS) {}
    }

    /** Summarize the last N messages in a room using AI. */
    suspend fun summarizeRoom(roomId: String, messageCount: Int = 50): String {
        return callService(ElementAppFunctionService.ACTION_SUMMARIZE_ROOM) {
            putExtra(ElementAppFunctionService.EXTRA_ROOM_ID, roomId)
            putExtra(ElementAppFunctionService.EXTRA_MESSAGE_COUNT, messageCount)
        }
    }

    /** Get unread message count and mentions across all rooms. */
    suspend fun getUnreadSummary(): String {
        return callService(ElementAppFunctionService.ACTION_UNREAD_SUMMARY) {}
    }

    private suspend fun callService(action: String, configure: Intent.() -> Unit): String {
        return withTimeout(SERVICE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val intent = Intent(context, ElementAppFunctionService::class.java).apply {
                    this.action = action
                    configure()
                    putExtra(ElementAppFunctionService.EXTRA_RESULT_RECEIVER, object : ResultReceiver(null) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            val result = if (resultCode == ElementAppFunctionService.RESULT_CODE_SUCCESS) {
                                resultData?.getString(ElementAppFunctionService.RESULT_KEY) ?: """{"error":"No result"}"""
                            } else {
                                val error = resultData?.getString(ElementAppFunctionService.ERROR_KEY) ?: "Unknown error"
                                """{"error":"$error"}"""
                            }
                            continuation.resume(result)
                        }
                    })
                }
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to start ElementAppFunctionService")
                    continuation.resume("""{"error":"Failed to start service: ${e.message}"}""")
                }
            }
        }
    }
}
