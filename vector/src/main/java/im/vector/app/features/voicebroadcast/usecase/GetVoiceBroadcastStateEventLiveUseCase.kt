/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.voiceBroadcastId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import timber.log.Timber
import javax.inject.Inject

class GetVoiceBroadcastStateEventLiveUseCase @Inject constructor(
        private val session: Session,
        private val getVoiceBroadcastStateEventUseCase: GetVoiceBroadcastStateEventUseCase,
) {

    fun execute(voiceBroadcast: VoiceBroadcast): Flow<Optional<VoiceBroadcastEvent>> {
        return getMostRecentVoiceBroadcastEventFlow(voiceBroadcast)
                .onEach { event ->
                    Timber.d(
                            "## VoiceBroadcast | " +
                                    "voiceBroadcastId=${event.getOrNull()?.voiceBroadcastId}, " +
                                    "state=${event.getOrNull()?.content?.voiceBroadcastState}"
                    )
                }
    }

    /**
     * Get a flow of the most recent event for the given voice broadcast.
     */
    private fun getMostRecentVoiceBroadcastEventFlow(voiceBroadcast: VoiceBroadcast): Flow<Optional<VoiceBroadcastEvent>> {
        val room = session.getRoom(voiceBroadcast.roomId) ?: error("Unknown roomId: ${voiceBroadcast.roomId}")
        val startedEventFlow = room.flow().liveTimelineEvent(voiceBroadcast.voiceBroadcastId)
        // observe started event changes
        return startedEventFlow
                .mapOptional { it.root.asVoiceBroadcastEvent() }
                .flatMapLatest { startedEvent ->
                    if (startedEvent.hasValue().not() || startedEvent.get().root.isRedacted()) {
                        // if started event is null or redacted, send null
                        flowOf(Optional.empty())
                    } else {
                        // otherwise, observe most recent event changes
                        getMostRecentRelatedEventFlow(room, voiceBroadcast)
                                .transformWhile { mostRecentEvent ->
                                    val hasValue = mostRecentEvent.hasValue()
                                    if (hasValue) {
                                        // keep the most recent event
                                        emit(mostRecentEvent)
                                    } else {
                                        // no most recent event, fallback to started event
                                        emit(startedEvent)
                                    }
                                    hasValue
                                }
                    }
                }
                .distinctUntilChangedBy { it.getOrNull()?.content?.voiceBroadcastState }
    }

    /**
     * Get a flow of the most recent related event.
     */
    private fun getMostRecentRelatedEventFlow(room: Room, voiceBroadcast: VoiceBroadcast): Flow<Optional<VoiceBroadcastEvent>> {
        val mostRecentEvent = getVoiceBroadcastStateEventUseCase.execute(voiceBroadcast).toOptional()
        return if (mostRecentEvent.hasValue()) {
            val stateKey = mostRecentEvent.get().root.stateKey.orEmpty()
            // observe incoming voice broadcast state events
            room.flow()
                    .liveStateEvent(VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO, QueryStringValue.Equals(stateKey))
                    .mapOptional { it.asVoiceBroadcastEvent() }
                    // drop first event sent by the matrix-sdk, we compute manually this first event
                    .drop(1)
                    // start with the computed most recent event
                    .onStart { emit(mostRecentEvent) }
                    // handle event if null or related to the given voice broadcast
                    .filter { it.hasValue().not() || it.get().voiceBroadcastId == voiceBroadcast.voiceBroadcastId }
                    // observe changes while event is not null
                    .transformWhile { event ->
                        emit(event)
                        event.hasValue()
                    }
                    .flatMapLatest { newMostRecentEvent ->
                        if (newMostRecentEvent.hasValue()) {
                            // observe most recent event changes
                            newMostRecentEvent.get().flow()
                                    .transformWhile { event ->
                                        // observe changes until event is null or redacted
                                        emit(event)
                                        event.hasValue() && event.get().root.isRedacted().not()
                                    }
                                    .flatMapLatest { event ->
                                        val isRedactedOrNull = !event.hasValue() || event.get().root.isRedacted()
                                        if (isRedactedOrNull) {
                                            // event is null or redacted, switch to the latest not redacted event
                                            getMostRecentRelatedEventFlow(room, voiceBroadcast)
                                        } else {
                                            // event is not redacted, send the event
                                            flowOf(event)
                                        }
                                    }
                        } else {
                            // there is no more most recent event, just send it
                            flowOf(newMostRecentEvent)
                        }
                    }
        } else {
            // there is no more most recent event, just send it
            flowOf(mostRecentEvent)
        }
    }

    /**
     * Get a flow of the given voice broadcast event changes.
     */
    private fun VoiceBroadcastEvent.flow(): Flow<Optional<VoiceBroadcastEvent>> {
        val room = this.root.roomId?.let { session.getRoom(it) } ?: return flowOf(Optional.empty())
        return room.flow().liveTimelineEvent(root.eventId!!).mapOptional { it.root.asVoiceBroadcastEvent() }
    }
}
