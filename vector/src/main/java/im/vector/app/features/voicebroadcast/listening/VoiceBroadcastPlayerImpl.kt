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

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import androidx.annotation.MainThread
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.onFirst
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.Listener
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.State
import im.vector.app.features.voicebroadcast.listening.usecase.GetLiveVoiceBroadcastChunksUseCase
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventLiveUseCase
import im.vector.lib.core.utils.timer.CountUpTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceBroadcastPlayerImpl @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val getVoiceBroadcastEventUseCase: GetVoiceBroadcastStateEventLiveUseCase,
        private val getLiveVoiceBroadcastChunksUseCase: GetLiveVoiceBroadcastChunksUseCase
) : VoiceBroadcastPlayer {

    private val session get() = sessionHolder.getActiveSession()
    private val sessionScope get() = session.coroutineScope

    private val mediaPlayerListener = MediaPlayerListener()
    private val playbackTicker = PlaybackTicker()
    private val playlist = VoiceBroadcastPlaylist()

    private var fetchPlaylistTask: Job? = null
    private var voiceBroadcastStateObserver: Job? = null

    private var currentMediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    private var isPreparingNextPlayer: Boolean = false

    private var mostRecentVoiceBroadcastEvent: VoiceBroadcastEvent? = null

    override var currentVoiceBroadcast: VoiceBroadcast? = null
    override var isLiveListening: Boolean = false
        @MainThread
        set(value) {
            if (field != value) {
                Timber.d("## Voice Broadcast | isLiveListening: $field -> $value")
                field = value
                onLiveListeningChanged(value)
            }
        }

    override var playingState = State.IDLE
        @MainThread
        set(value) {
            if (field != value) {
                Timber.d("## Voice Broadcast | playingState: $field -> $value")
                field = value
                onPlayingStateChanged(value)
            }
        }

    /** Map voiceBroadcastId to listeners. */
    private val listeners: MutableMap<String, CopyOnWriteArrayList<Listener>> = mutableMapOf()

    override fun playOrResume(voiceBroadcast: VoiceBroadcast) {
        val hasChanged = currentVoiceBroadcast != voiceBroadcast
        when {
            hasChanged -> startPlayback(voiceBroadcast)
            playingState == State.PAUSED -> resumePlayback()
            else -> Unit
        }
    }

    override fun pause() {
        pausePlayback()
    }

    override fun stop() {
        // Update state
        playingState = State.IDLE

        // Stop and release media players
        stopPlayer()

        // Do not observe anymore voice broadcast changes
        fetchPlaylistTask?.cancel()
        fetchPlaylistTask = null
        voiceBroadcastStateObserver?.cancel()
        voiceBroadcastStateObserver = null

        // Clear playlist
        playlist.reset()

        mostRecentVoiceBroadcastEvent = null
        currentVoiceBroadcast = null
    }

    override fun addListener(voiceBroadcast: VoiceBroadcast, listener: Listener) {
        listeners[voiceBroadcast.voiceBroadcastId]?.add(listener) ?: run {
            listeners[voiceBroadcast.voiceBroadcastId] = CopyOnWriteArrayList<Listener>().apply { add(listener) }
        }
        listener.onPlayingStateChanged(if (voiceBroadcast == currentVoiceBroadcast) playingState else State.IDLE)
        listener.onLiveModeChanged(voiceBroadcast == currentVoiceBroadcast)
    }

    override fun removeListener(voiceBroadcast: VoiceBroadcast, listener: Listener) {
        listeners[voiceBroadcast.voiceBroadcastId]?.remove(listener)
    }

    private fun startPlayback(voiceBroadcast: VoiceBroadcast) {
        // Stop listening previous voice broadcast if any
        if (playingState != State.IDLE) stop()

        currentVoiceBroadcast = voiceBroadcast

        playingState = State.BUFFERING

        observeVoiceBroadcastStateEvent(voiceBroadcast)
    }

    private fun observeVoiceBroadcastStateEvent(voiceBroadcast: VoiceBroadcast) {
        voiceBroadcastStateObserver = getVoiceBroadcastEventUseCase.execute(voiceBroadcast)
                .onFirst { fetchPlaylistAndStartPlayback(voiceBroadcast) }
                .onEach { onVoiceBroadcastStateEventUpdated(it.getOrNull()) }
                .launchIn(sessionScope)
    }

    private fun onVoiceBroadcastStateEventUpdated(event: VoiceBroadcastEvent?) {
        if (event == null) {
            stop()
        } else {
            mostRecentVoiceBroadcastEvent = event
            updateLiveListeningMode()
        }
    }

    private fun fetchPlaylistAndStartPlayback(voiceBroadcast: VoiceBroadcast) {
        fetchPlaylistTask = getLiveVoiceBroadcastChunksUseCase.execute(voiceBroadcast)
                .onEach {
                    playlist.setItems(it)
                    onPlaylistUpdated()
                }
                .launchIn(sessionScope)
    }

    private fun onPlaylistUpdated() {
        when (playingState) {
            State.PLAYING,
            State.PAUSED -> {
                if (nextMediaPlayer == null && !isPreparingNextPlayer) {
                    prepareNextMediaPlayer()
                }
            }
            State.BUFFERING -> {
                val nextItem = if (isLiveListening && playlist.currentSequence == null) {
                    // live listening, jump to the last item if playback has not started
                    playlist.lastOrNull()
                } else {
                    // not live or playback already started, request next item
                    playlist.getNextItem()
                }
                if (nextItem != null) {
                    startPlayback(nextItem.startTime)
                }
            }
            State.IDLE -> Unit // Should not happen
        }
    }

    private fun startPlayback(position: Int) {
        stopPlayer()

        val playlistItem = playlist.findByPosition(position)
        val content = playlistItem?.audioEvent?.content ?: run { Timber.w("## Voice Broadcast | No content to play at position $position"); return }
        val sequence = playlistItem.sequence ?: run { Timber.w("## Voice Broadcast | Playlist item has no sequence"); return }
        val sequencePosition = position - playlistItem.startTime
        sessionScope.launch {
            try {
                prepareMediaPlayer(content) { mp ->
                    currentMediaPlayer = mp
                    playlist.currentSequence = sequence
                    mp.start()
                    if (sequencePosition > 0) {
                        mp.seekTo(sequencePosition)
                    }
                    playingState = State.PLAYING
                    prepareNextMediaPlayer()
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "## Voice Broadcast | Unable to start playback: $failure")
                throw VoiceFailure.UnableToPlay(failure)
            }
        }
    }

    private fun pausePlayback() {
        playingState = State.PAUSED // This will trigger a playing state update and save the current position
        if (currentMediaPlayer != null) {
            currentMediaPlayer?.pause()
        } else {
            stopPlayer()
        }
    }

    private fun resumePlayback() {
        if (currentMediaPlayer != null) {
            playingState = State.PLAYING
            currentMediaPlayer?.start()
        } else {
            val savedPosition = currentVoiceBroadcast?.voiceBroadcastId?.let { playbackTracker.getPlaybackTime(it) } ?: 0
            startPlayback(savedPosition)
        }
    }

    override fun seekTo(voiceBroadcast: VoiceBroadcast, positionMillis: Int, duration: Int) {
        when {
            voiceBroadcast != currentVoiceBroadcast -> {
                playbackTracker.updatePausedAtPlaybackTime(voiceBroadcast.voiceBroadcastId, positionMillis, positionMillis.toFloat() / duration)
            }
            playingState == State.PLAYING || playingState == State.BUFFERING -> {
                updateLiveListeningMode(positionMillis)
                startPlayback(positionMillis)
            }
            playingState == State.IDLE || playingState == State.PAUSED -> {
                stopPlayer()
                playbackTracker.updatePausedAtPlaybackTime(voiceBroadcast.voiceBroadcastId, positionMillis, positionMillis.toFloat() / duration)
            }
        }
    }

    private fun prepareNextMediaPlayer() {
        val nextItem = playlist.getNextItem()
        if (nextItem != null) {
            isPreparingNextPlayer = true
            sessionScope.launch {
                prepareMediaPlayer(nextItem.audioEvent.content) { mp ->
                    isPreparingNextPlayer = false
                    nextMediaPlayer = mp
                    when (playingState) {
                        State.PLAYING,
                        State.PAUSED -> {
                            currentMediaPlayer?.setNextMediaPlayer(mp)
                        }
                        State.BUFFERING -> {
                            mp.start()
                            onNextMediaPlayerStarted(mp)
                        }
                        State.IDLE -> stopPlayer()
                    }
                }
            }
        }
    }

    private suspend fun prepareMediaPlayer(messageAudioContent: MessageAudioContent, onPreparedListener: OnPreparedListener): MediaPlayer {
        // Download can fail
        val audioFile = try {
            session.fileService().downloadFile(messageAudioContent)
        } catch (failure: Throwable) {
            Timber.e(failure, "Voice Broadcast | Download has failed: $failure")
            throw VoiceFailure.UnableToPlay(failure)
        }

        return audioFile.inputStream().use { fis ->
            MediaPlayer().apply {
                setAudioAttributes(
                        AudioAttributes.Builder()
                                // Do not use CONTENT_TYPE_SPEECH / USAGE_VOICE_COMMUNICATION because we want to play loud here
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                )
                setDataSource(fis.fd)
                setOnInfoListener(mediaPlayerListener)
                setOnErrorListener(mediaPlayerListener)
                setOnPreparedListener(onPreparedListener)
                setOnCompletionListener(mediaPlayerListener)
                prepare()
            }
        }
    }

    private fun stopPlayer() {
        tryOrNull { currentMediaPlayer?.stop() }
        currentMediaPlayer?.release()
        currentMediaPlayer = null

        nextMediaPlayer?.release()
        nextMediaPlayer = null
        isPreparingNextPlayer = false
    }

    private fun onPlayingStateChanged(playingState: State) {
        // Update live playback flag
        updateLiveListeningMode()

        currentVoiceBroadcast?.voiceBroadcastId?.let { voiceBroadcastId ->
            // Start or stop playback ticker
            when (playingState) {
                State.PLAYING -> playbackTicker.startPlaybackTicker(voiceBroadcastId)
                State.PAUSED,
                State.BUFFERING,
                State.IDLE -> playbackTicker.stopPlaybackTicker(voiceBroadcastId)
            }
            // Notify state change to all the listeners attached to the current voice broadcast id
            listeners[voiceBroadcastId]?.forEach { listener -> listener.onPlayingStateChanged(playingState) }
        }
    }

    /**
     * Update the live listening state according to:
     * - the voice broadcast state (started/paused/resumed/stopped),
     * - the playing state (IDLE, PLAYING, PAUSED, BUFFERING),
     * - the potential seek position (backward/forward).
     */
    private fun updateLiveListeningMode(seekPosition: Int? = null) {
        isLiveListening = when {
            // the current voice broadcast is not live (ended)
            mostRecentVoiceBroadcastEvent?.isLive != true -> false
            // the player is stopped or paused
            playingState == State.IDLE || playingState == State.PAUSED -> false
            seekPosition != null -> {
                val seekDirection = seekPosition.compareTo(getCurrentPlaybackPosition() ?: 0)
                val newSequence = playlist.findByPosition(seekPosition)?.sequence
                // the user has sought forward
                if (seekDirection >= 0) {
                    // stay in live or latest sequence reached
                    isLiveListening || newSequence == playlist.lastOrNull()?.sequence
                }
                // the user has sought backward
                else {
                    // was in live and stay in the same sequence
                    isLiveListening && newSequence == playlist.currentSequence
                }
            }
            // if there is no saved position, go in live
            getCurrentPlaybackPosition() == null -> true
            // if we reached the latest sequence, go in live
            playlist.currentSequence == playlist.lastOrNull()?.sequence -> true
            // otherwise, do not change
            else -> isLiveListening
        }
    }

    private fun onLiveListeningChanged(isLiveListening: Boolean) {
        // Live has ended and last chunk has been reached, we can stop the playback
        if (!isLiveListening && playingState == State.BUFFERING && playlist.currentSequence == mostRecentVoiceBroadcastEvent?.content?.lastChunkSequence) {
            stop()
        }
    }

    private fun onNextMediaPlayerStarted(mp: MediaPlayer) {
        playingState = State.PLAYING
        playlist.currentSequence = playlist.currentSequence?.inc()
        currentMediaPlayer = mp
        nextMediaPlayer = null
        prepareNextMediaPlayer()
    }

    private fun getCurrentPlaybackPosition(): Int? {
        val voiceBroadcastId = currentVoiceBroadcast?.voiceBroadcastId ?: return null
        val computedPosition = currentMediaPlayer?.currentPosition?.let { playlist.currentItem?.startTime?.plus(it) }
        val savedPosition = playbackTracker.getPlaybackTime(voiceBroadcastId)
        return computedPosition ?: savedPosition
    }

    private fun getCurrentPlaybackPercentage(): Float? {
        val playlistPosition = playlist.currentItem?.startTime
        val computedPosition = currentMediaPlayer?.currentPosition?.let { playlistPosition?.plus(it) } ?: playlistPosition
        val duration = playlist.duration.takeIf { it > 0 }
        val computedPercentage = if (computedPosition != null && duration != null) computedPosition.toFloat() / duration else null
        val savedPercentage = currentVoiceBroadcast?.voiceBroadcastId?.let { playbackTracker.getPercentage(it) }
        return computedPercentage ?: savedPercentage
    }

    private inner class MediaPlayerListener :
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener {

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            when (what) {
                MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT -> onNextMediaPlayerStarted(mp)
            }
            return false
        }

        override fun onCompletion(mp: MediaPlayer) {
            // Next media player is already attached to this player and will start playing automatically
            if (nextMediaPlayer != null) return

            val hasEnded = !isLiveListening && mostRecentVoiceBroadcastEvent?.content?.lastChunkSequence == playlist.currentSequence
            if (hasEnded) {
                // We'll not receive new chunks anymore so we can stop the live listening
                stop()
            } else {
                // Enter in buffering mode and release current media player
                playingState = State.BUFFERING
                currentMediaPlayer?.release()
                currentMediaPlayer = null
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            stop()
            return true
        }
    }

    private inner class PlaybackTicker(
            private var playbackTicker: CountUpTimer? = null,
    ) {

        fun startPlaybackTicker(id: String) {
            playbackTicker?.stop()
            playbackTicker = CountUpTimer(50L).apply {
                tickListener = CountUpTimer.TickListener { onPlaybackTick(id) }
                resume()
            }
            onPlaybackTick(id)
        }

        fun stopPlaybackTicker(id: String) {
            playbackTicker?.stop()
            playbackTicker = null
            onPlaybackTick(id)
        }

        private fun onPlaybackTick(id: String) {
            val playbackTime = getCurrentPlaybackPosition()
            val percentage = getCurrentPlaybackPercentage()
            when (playingState) {
                State.PLAYING -> {
                    if (playbackTime != null && percentage != null) {
                        playbackTracker.updatePlayingAtPlaybackTime(id, playbackTime, percentage)
                    }
                }
                State.PAUSED,
                State.BUFFERING -> {
                    if (playbackTime != null && percentage != null) {
                        playbackTracker.updatePausedAtPlaybackTime(id, playbackTime, percentage)
                    }
                }
                State.IDLE -> {
                    if (playbackTime == null || percentage == null || (playlist.duration - playbackTime) < 50) {
                        playbackTracker.stopPlayback(id)
                    } else {
                        playbackTracker.updatePausedAtPlaybackTime(id, playbackTime, percentage)
                    }
                }
            }
        }
    }
}
