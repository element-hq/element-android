/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.send.queue

import android.content.Context
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Simple lightweight persistence
 * Don't want to go in DB due to current issues
 * Will never manage lots of events, it simply uses sharedPreferences.
 * It is just used to remember what events/localEchos was managed by the event sender in order to
 * reschedule them (and only them) on next restart
 */

private const val PERSISTENCE_KEY = "ManagedBySender"

internal class QueueMemento @Inject constructor(context: Context,
                                                @SessionId sessionId: String,
                                                private val queuedTaskFactory: QueuedTaskFactory,
                                                private val localEchoRepository: LocalEchoRepository,
                                                private val cryptoService: CryptoService) {

    private val storage = context.getSharedPreferences("QueueMemento_$sessionId", Context.MODE_PRIVATE)
    private val trackedTasks = mutableListOf<QueuedTask>()

    fun track(task: QueuedTask) = synchronized(trackedTasks) {
        trackedTasks.add(task)
        persist()
    }

    fun unTrack(task: QueuedTask) = synchronized(trackedTasks) {
        trackedTasks.remove(task)
        persist()
    }

    fun trackedTasks() = synchronized(trackedTasks) {
    }

    private fun persist() {
        trackedTasks.mapIndexedNotNull { index, queuedTask ->
            toTaskInfo(queuedTask, index)?.let { TaskInfo.map(it) }
        }.toSet().let { set ->
            storage.edit()
                    .putStringSet(PERSISTENCE_KEY, set)
                    .apply()
        }
    }

    private fun toTaskInfo(task: QueuedTask, order: Int): TaskInfo? {
        return when (task) {
            is SendEventQueuedTask -> SendEventTaskInfo(
                    localEchoId = task.event.eventId ?: "",
                    encrypt = task.encrypt,
                    order = order
            )
            is RedactQueuedTask -> RedactEventTaskInfo(
                    redactionLocalEcho = task.redactionLocalEchoId,
                    order = order
            )
            else                   -> null
        }
    }

    suspend fun restoreTasks(eventProcessor: EventSenderProcessor) {
        // events should be restarted in correct order
        storage.getStringSet(PERSISTENCE_KEY, null)?.let { pending ->
            Timber.d("## Send - Recovering unsent events $pending")
            pending.mapNotNull { tryOrNull { TaskInfo.map(it) } }
        }
                ?.sortedBy { it.order }
                ?.forEach { info ->
                    try {
                        when (info) {
                            is SendEventTaskInfo -> {
                                localEchoRepository.getUpToDateEcho(info.localEchoId)?.let {
                                    if (it.sendState.isSending() && it.eventId != null && it.roomId != null) {
                                        localEchoRepository.updateSendState(it.eventId, it.roomId, SendState.UNSENT)
                                        Timber.d("## Send -Reschedule send $info")
                                        eventProcessor.postTask(queuedTaskFactory.createSendTask(it, info.encrypt ?: cryptoService.isRoomEncrypted(it.roomId)))
                                    }
                                }
                            }
                            is RedactEventTaskInfo -> {
                                info.redactionLocalEcho?.let { localEchoRepository.getUpToDateEcho(it) }?.let {
                                    localEchoRepository.updateSendState(it.eventId!!, it.roomId, SendState.UNSENT)
                                    // try to get reason
                                    val reason = it.content?.get("reason") as? String
                                    if (it.redacts != null && it.roomId != null) {
                                        Timber.d("## Send -Reschedule redact $info")
                                        eventProcessor.postTask(queuedTaskFactory.createRedactTask(it.eventId, it.redacts, it.roomId, reason))
                                    }
                                }
                                // postTask(queuedTaskFactory.createRedactTask(info.eventToRedactId, info.)
                            }
                        }
                    } catch (failure: Throwable) {
                        Timber.e("failed to restore task $info")
                    }
                }
    }
}
