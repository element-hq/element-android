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
import org.matrix.android.sdk.api.session.events.model.RelationType
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

    fun execute(voiceBroadcast: VoiceBroadcast): Flow<List<MessageAudioEvent>> {
        val session = activeSessionHolder.getSafeActiveSession() ?: return emptyFlow()
        val room = session.roomService().getRoom(voiceBroadcast.roomId) ?: return emptyFlow()
        val timeline = room.timelineService().createTimeline(null, TimelineSettings(5))

        // Get initial chunks
        val existingChunks = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, voiceBroadcast.voiceBroadcastId)
                .mapNotNull { timelineEvent -> timelineEvent.root.asMessageAudioEvent().takeIf { it.isVoiceBroadcast() } }

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
                        if (lastSequence != null && newChunks.any { it.sequence == lastSequence }) {
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
                    .runningReduce { accumulator: List<MessageAudioEvent>, value: List<MessageAudioEvent> -> accumulator.plus(value) }
                    .map { events -> events.distinctBy { it.sequence } }
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
    private fun List<TimelineEvent>.mapToChunkEvents(voiceBroadcastId: String, senderId: String?): List<MessageAudioEvent> =
            this.mapNotNull { timelineEvent ->
                timelineEvent.root.asMessageAudioEvent()
                        ?.takeIf {
                            it.isVoiceBroadcast() && it.getVoiceBroadcastEventId() == voiceBroadcastId &&
                                    it.root.senderId == senderId
                        }
            }
}
