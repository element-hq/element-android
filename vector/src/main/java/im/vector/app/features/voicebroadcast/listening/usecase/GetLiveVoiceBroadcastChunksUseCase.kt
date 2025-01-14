/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.listening.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.voicebroadcast.getVoiceBroadcastEventId
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.sequence
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventUseCase
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

/**
 * Get a [Flow] of [MessageAudioEvent]s related to the given voice broadcast.
 */
class GetLiveVoiceBroadcastChunksUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val getVoiceBroadcastEventUseCase: GetVoiceBroadcastStateEventUseCase,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): Flow<List<Event>> {
        val session = activeSessionHolder.getSafeActiveSession() ?: return emptyFlow()
        val room = session.roomService().getRoom(voiceBroadcast.roomId) ?: return emptyFlow()
        val timeline = room.timelineService().createTimeline(null, TimelineSettings(5))

        // Get initial chunks
        val existingChunks = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                .mapNotNull { timelineEvent ->
                    val event = timelineEvent.root
                    val relationContent = event.getRelationContent()
                    when {
                        event.getClearType() == EventType.MESSAGE -> event.takeIf { it.asMessageAudioEvent().isVoiceBroadcast() }
                        event.getClearType() == EventType.ENCRYPTED && relationContent?.type == RelationType.REFERENCE -> event
                        else -> null
                    }
                }

        val voiceBroadcastEvent = getVoiceBroadcastEventUseCase.execute(voiceBroadcast)
        val voiceBroadcastState = voiceBroadcastEvent?.content?.voiceBroadcastState

        return if (voiceBroadcastState == null || voiceBroadcastState == VoiceBroadcastState.STOPPED) {
            // Just send the existing chunks if voice broadcast is stopped
            flowOf(existingChunks)
        } else {
            // Observe new timeline events if voice broadcast is ongoing
            callbackFlow {
                // Init with existing chunks
                send(existingChunks)

                // Observe new timeline events
                val listener = object : Timeline.Listener {
                    private var latestEventId: String? = null
                    private var lastSequence: Int? = null

                    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                        val latestEventIndex = latestEventId?.let { eventId -> snapshot.indexOfFirst { it.eventId == eventId } }
                        val newEvents = if (latestEventIndex != null) snapshot.subList(0, latestEventIndex) else snapshot

                        // Detect a potential stopped voice broadcast state event
                        val stopEvent = newEvents.findStopEvent(voiceBroadcast)
                        if (stopEvent != null) {
                            lastSequence = stopEvent.content?.lastChunkSequence
                        }

                        val newChunks = newEvents.mapToChunkEvents(voiceBroadcast.voiceBroadcastId, voiceBroadcastEvent.root.senderId)

                        // Notify about new chunks
                        if (newChunks.isNotEmpty()) {
                            trySend(newChunks)
                        }

                        // Automatically stop observing the timeline if the last chunk has been received
                        if (lastSequence != null && newChunks.any { it.asMessageAudioEvent()?.sequence == lastSequence }) {
                            timeline.removeListener(this)
                            timeline.dispose()
                        }

                        latestEventId = snapshot.firstOrNull()?.eventId
                    }
                }

                timeline.addListener(listener)
                timeline.start()
                awaitClose {
                    timeline.removeListener(listener)
                    timeline.dispose()
                }
            }
                    .runningReduce { accumulator: List<Event>, value: List<Event> -> accumulator.plus(value) }
                    .map { events -> events.distinctBy { it.eventId } }
        }
    }

    /**
     * Find a [VoiceBroadcastEvent] with a [VoiceBroadcastState.STOPPED] state.
     */
    private fun List<TimelineEvent>.findStopEvent(voiceBroadcast: VoiceBroadcast): VoiceBroadcastEvent? =
            this.mapNotNull { timelineEvent -> timelineEvent.root.asVoiceBroadcastEvent()?.takeIf { it.voiceBroadcastId == voiceBroadcast.voiceBroadcastId } }
                    .find { it.content?.voiceBroadcastState == VoiceBroadcastState.STOPPED }

    /**
     * Transform the list of [TimelineEvent] to a mapped list of [MessageAudioEvent] related to a given voice broadcast.
     */
    private fun List<TimelineEvent>.mapToChunkEvents(voiceBroadcastId: String, senderId: String?): List<Event> =
            this.mapNotNull { timelineEvent ->
                val event = timelineEvent.root
                val relationContent = event.getRelationContent()
                when {
                    event.getClearType() == EventType.MESSAGE -> {
                        event.asMessageAudioEvent()
                                ?.takeIf {
                                    it.isVoiceBroadcast() && it.getVoiceBroadcastEventId() == voiceBroadcastId && it.root.senderId == senderId
                                }?.root
                    }
                    event.getClearType() == EventType.ENCRYPTED && relationContent?.type == RelationType.REFERENCE -> {
                        event.takeIf { relationContent.eventId == voiceBroadcastId }
                    }
                    else -> null
                }
            }
}
