/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
