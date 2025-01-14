/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.listening

import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast

interface VoiceBroadcastPlayer {

    /**
     * The current playing voice broadcast, if any.
     */
    val currentVoiceBroadcast: VoiceBroadcast?

    /**
     * The current playing [State], [State.Idle] by default.
     */
    val playingState: State

    /**
     * Tells whether the player is listening a live voice broadcast in "live" position.
     */
    val isLiveListening: Boolean

    /**
     * Start playback of the given voice broadcast.
     */
    fun playOrResume(voiceBroadcast: VoiceBroadcast)

    /**
     * Pause playback of the current voice broadcast, if any.
     */
    fun pause()

    /**
     * Stop playback of the current voice broadcast, if any, and reset the player state.
     */
    fun stop()

    /**
     * Seek the given voice broadcast playback to the given position, is milliseconds.
     */
    fun seekTo(voiceBroadcast: VoiceBroadcast, positionMillis: Int, duration: Int)

    /**
     * Add a [Listener] to the given voice broadcast.
     */
    fun addListener(voiceBroadcast: VoiceBroadcast, listener: Listener)

    /**
     * Remove a [Listener] from the given voice broadcast.
     */
    fun removeListener(voiceBroadcast: VoiceBroadcast, listener: Listener)

    /**
     * Player states.
     */
    sealed interface State {
        object Playing : State
        object Paused : State
        object Buffering : State
        data class Error(val failure: VoiceBroadcastFailure.ListeningError) : State
        object Idle : State
    }

    /**
     * Listener related to [VoiceBroadcastPlayer].
     */
    interface Listener {
        /**
         * Notify about [VoiceBroadcastPlayer.playingState] changes.
         */
        fun onPlayingStateChanged(state: State) = Unit

        /**
         * Notify about [VoiceBroadcastPlayer.isLiveListening] changes.
         */
        fun onLiveModeChanged(isLive: Boolean) = Unit
    }
}
