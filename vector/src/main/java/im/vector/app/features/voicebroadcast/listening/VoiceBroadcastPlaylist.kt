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

package im.vector.app.features.voicebroadcast.listening

import im.vector.app.features.voicebroadcast.duration
import im.vector.app.features.voicebroadcast.sequence
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent

class VoiceBroadcastPlaylist(
        private val items: MutableList<PlaylistItem> = mutableListOf(),
) : List<PlaylistItem> by items {

    var currentSequence: Int? = null
    val currentItem get() = currentSequence?.let { findBySequence(it) }

    val duration
        get() = items.lastOrNull()?.let { it.startTime + it.audioEvent.duration } ?: 0

    fun setItems(audioEvents: List<MessageAudioEvent>) {
        items.clear()
        val sorted = audioEvents.sortedBy { it.sequence?.toLong() ?: it.root.originServerTs }
        val chunkPositions = sorted
                .map { it.duration }
                .runningFold(0) { acc, i -> acc + i }
                .dropLast(1)
        val newItems = sorted.mapIndexed { index, messageAudioEvent ->
            PlaylistItem(
                    audioEvent = messageAudioEvent,
                    startTime = chunkPositions.getOrNull(index) ?: 0
            )
        }
        items.addAll(newItems)
    }

    fun reset() {
        currentSequence = null
        items.clear()
    }

    fun findByPosition(positionMillis: Int): PlaylistItem? {
        return items.lastOrNull { it.startTime <= positionMillis }
    }

    fun findBySequence(sequenceNumber: Int): PlaylistItem? {
        return items.find { it.sequence == sequenceNumber }
    }

    fun getNextItem() = findBySequence(currentSequence?.plus(1) ?: 1)

    fun firstOrNull() = findBySequence(1)
}

data class PlaylistItem(val audioEvent: MessageAudioEvent, val startTime: Int) {
    val sequence: Int? = audioEvent.sequence
    val duration: Int = audioEvent.duration
}
