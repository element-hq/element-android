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
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.duration
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.Listener
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.State
import im.vector.app.features.voicebroadcast.listening.usecase.GetLiveVoiceBroadcastChunksUseCase
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.sequence
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastEventUseCase
import im.vector.lib.core.utils.timer.CountUpTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class VoiceBroadcastPlayerImpl @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val getVoiceBroadcastEventUseCase: GetVoiceBroadcastEventUseCase,
        private val getLiveVoiceBroadcastChunksUseCase: GetLiveVoiceBroadcastChunksUseCase
) : VoiceBroadcastPlayer {

    private val session
        get() = sessionHolder.getActiveSession()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var fetchPlaylistTask: Job? = null
    private var voiceBroadcastStateTask: Job? = null

    private val mediaPlayerListener = MediaPlayerListener()
    private val playbackTicker = PlaybackTicker()

    private var currentMediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null

    private var playlist = emptyList<PlaylistItem>()
    private var currentSequence: Int? = null
    private var currentVoiceBroadcastEvent: VoiceBroadcastEvent? = null
    private val isLive get() = currentVoiceBroadcastEvent?.isLive.orFalse()
    private val lastSequence get() = currentVoiceBroadcastEvent?.content?.lastChunkSequence

    override var currentVoiceBroadcast: VoiceBroadcast? = null

    override var playingState = State.IDLE
        @MainThread
        set(value) {
            Timber.w("## VoiceBroadcastPlayer state: $field -> $value")
            field = value
            onPlayingStateChanged(value)
        }

    /** Map voiceBroadcastId to listeners.*/
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
        currentMediaPlayer?.pause()
        playingState = State.PAUSED
    }

    override fun stop() {
        // Update state
        playingState = State.IDLE

        // Stop and release media players
        stopPlayer()

        // Do not observe anymore voice broadcast changes
        fetchPlaylistTask?.cancel()
        fetchPlaylistTask = null
        voiceBroadcastStateTask?.cancel()
        voiceBroadcastStateTask = null

        // Clear playlist
        playlist = emptyList()
        currentSequence = null

        currentVoiceBroadcastEvent = null
        currentVoiceBroadcast = null
    }

    override fun addListener(voiceBroadcast: VoiceBroadcast, listener: Listener) {
        listeners[voiceBroadcast.voiceBroadcastId]?.add(listener) ?: run {
            listeners[voiceBroadcast.voiceBroadcastId] = CopyOnWriteArrayList<Listener>().apply { add(listener) }
        }
        listener.onStateChanged(if (voiceBroadcast == currentVoiceBroadcast) playingState else State.IDLE)
    }

    override fun removeListener(voiceBroadcast: VoiceBroadcast, listener: Listener) {
        listeners[voiceBroadcast.voiceBroadcastId]?.remove(listener)
    }

    private fun startPlayback(voiceBroadcast: VoiceBroadcast) {
        // Stop listening previous voice broadcast if any
        if (playingState != State.IDLE) stop()

        currentVoiceBroadcast = voiceBroadcast

        playingState = State.BUFFERING

        observeVoiceBroadcastLiveState(voiceBroadcast)
        fetchPlaylistAndStartPlayback(voiceBroadcast)
    }

    private fun observeVoiceBroadcastLiveState(voiceBroadcast: VoiceBroadcast) {
        voiceBroadcastStateTask = getVoiceBroadcastEventUseCase.execute(voiceBroadcast)
                .onEach { currentVoiceBroadcastEvent = it.getOrNull() }
                .launchIn(coroutineScope)
    }

    private fun fetchPlaylistAndStartPlayback(voiceBroadcast: VoiceBroadcast) {
        fetchPlaylistTask = getLiveVoiceBroadcastChunksUseCase.execute(voiceBroadcast)
                .onEach(this::updatePlaylist)
                .launchIn(coroutineScope)
    }

    private fun updatePlaylist(audioEvents: List<MessageAudioEvent>) {
        val sorted = audioEvents.sortedBy { it.sequence?.toLong() ?: it.root.originServerTs }
        val chunkPositions = sorted
                .map { it.duration }
                .runningFold(0) { acc, i -> acc + i }
                .dropLast(1)
        playlist = sorted.mapIndexed { index, messageAudioEvent ->
            PlaylistItem(
                    audioEvent = messageAudioEvent,
                    startTime = chunkPositions.getOrNull(index) ?: 0
            )
        }
        onPlaylistUpdated()
    }

    private fun onPlaylistUpdated() {
        when (playingState) {
            State.PLAYING -> {
                if (nextMediaPlayer == null) {
                    prepareNextMediaPlayer()
                }
            }
            State.PAUSED -> {
                if (nextMediaPlayer == null) {
                    prepareNextMediaPlayer()
                }
            }
            State.BUFFERING -> {
                val newMediaContent = getNextAudioContent()
                if (newMediaContent != null) {
                    val savedPosition = currentVoiceBroadcast?.let { playbackTracker.getPlaybackTime(it.voiceBroadcastId) }
                    startPlayback(savedPosition)
                }
            }
            State.IDLE -> {
                val savedPosition = currentVoiceBroadcast?.let { playbackTracker.getPlaybackTime(it.voiceBroadcastId) }
                startPlayback(savedPosition)
            }
        }
    }

    private fun startPlayback(position: Int? = null) {
        stopPlayer()

        val playlistItem = when {
            position != null -> playlist.lastOrNull { it.startTime <= position }
            isLive -> playlist.lastOrNull()
            else -> playlist.firstOrNull()
        }
        val content = playlistItem?.audioEvent?.content ?: run { Timber.w("## VoiceBroadcastPlayer: No content to play"); return }
        val sequence = playlistItem.audioEvent.sequence
        val sequencePosition = position?.let { it - playlistItem.startTime } ?: 0
        coroutineScope.launch {
            try {
                prepareMediaPlayer(content) { mp ->
                    currentMediaPlayer = mp
                    currentSequence = sequence
                    mp.start()
                    if (sequencePosition > 0) {
                        mp.seekTo(sequencePosition)
                    }
                    playingState = State.PLAYING
                    prepareNextMediaPlayer()
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Unable to start playback")
                throw VoiceFailure.UnableToPlay(failure)
            }
        }
    }

    private fun resumePlayback() {
        currentMediaPlayer?.start()
        playingState = State.PLAYING
    }

    override fun seekTo(voiceBroadcast: VoiceBroadcast, positionMillis: Int) {
        if (voiceBroadcast != currentVoiceBroadcast) {
            playbackTracker.updatePausedAtPlaybackTime(voiceBroadcast.voiceBroadcastId, positionMillis, 0f)
        } else {
            startPlayback(positionMillis)
        }
    }

    private fun getNextAudioContent(): MessageAudioContent? {
        val nextSequence = currentSequence?.plus(1)
                ?: playlist.lastOrNull()?.audioEvent?.sequence
                ?: 1
        return playlist.find { it.audioEvent.sequence == nextSequence }?.audioEvent?.content
    }

    private fun prepareNextMediaPlayer() {
        nextMediaPlayer = null
        val nextContent = getNextAudioContent()
        if (nextContent != null) {
            coroutineScope.launch {
                prepareMediaPlayer(nextContent) { mp ->
                    if (nextMediaPlayer == null) {
                        nextMediaPlayer = mp
                        currentMediaPlayer?.setNextMediaPlayer(mp)
                    } else {
                        mp.release()
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
            Timber.e(failure, "Unable to start playback")
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
    }

    private fun onPlayingStateChanged(playingState: State) {
        // Notify state change to all the listeners attached to the current voice broadcast id
        currentVoiceBroadcast?.voiceBroadcastId?.let { voiceBroadcastId ->
            when (playingState) {
                State.PLAYING -> playbackTicker.startPlaybackTicker(voiceBroadcastId)
                State.PAUSED,
                State.BUFFERING,
                State.IDLE -> playbackTicker.stopPlaybackTicker(voiceBroadcastId)
            }
            listeners[voiceBroadcastId]?.forEach { listener -> listener.onStateChanged(playingState) }
        }
    }

    private inner class MediaPlayerListener :
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener {

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            when (what) {
                MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT -> {
                    currentSequence = currentSequence?.plus(1)
                    currentMediaPlayer = mp
                    prepareNextMediaPlayer()
                }
            }
            return false
        }

        override fun onCompletion(mp: MediaPlayer) {
            if (nextMediaPlayer != null) return

            if (!isLive && lastSequence == currentSequence) {
                // We'll not receive new chunks anymore so we can stop the live listening
                stop()
            } else {
                playingState = State.BUFFERING
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            stop()
            return true
        }
    }

    private fun getVoiceBroadcastDuration() = playlist.lastOrNull()?.let { it.startTime + it.audioEvent.duration } ?: 0

    private data class PlaylistItem(val audioEvent: MessageAudioEvent, val startTime: Int)

    private inner class PlaybackTicker(
            private var playbackTicker: CountUpTimer? = null,
    ) {

        fun startPlaybackTicker(id: String) {
            playbackTicker?.stop()
            playbackTicker = CountUpTimer().apply {
                tickListener = object : CountUpTimer.TickListener {
                    override fun onTick(milliseconds: Long) {
                        onPlaybackTick(id)
                    }
                }
                resume()
            }
            onPlaybackTick(id)
        }

        private fun onPlaybackTick(id: String) {
            if (currentMediaPlayer?.isPlaying.orFalse()) {
                val itemStartPosition = currentSequence?.let { seq -> playlist.find { it.audioEvent.sequence == seq } }?.startTime
                val currentVoiceBroadcastPosition = itemStartPosition?.plus(currentMediaPlayer?.currentPosition ?: 0)
                Timber.d("Voice Broadcast | VoiceBroadcastPlayerImpl - sequence: $currentSequence, itemStartPosition $itemStartPosition, currentMediaPlayer=$currentMediaPlayer, currentMediaPlayer?.currentPosition: ${currentMediaPlayer?.currentPosition}")
                if (currentVoiceBroadcastPosition != null) {
                    val totalDuration = getVoiceBroadcastDuration()
                    val percentage = currentVoiceBroadcastPosition.toFloat() / totalDuration
                    playbackTracker.updatePlayingAtPlaybackTime(id, currentVoiceBroadcastPosition, percentage)
                } else {
                    stopPlaybackTicker(id)
                }
            } else {
                stopPlaybackTicker(id)
            }
        }

        fun stopPlaybackTicker(id: String) {
            playbackTicker?.stop()
            playbackTicker = null

            val totalDuration = getVoiceBroadcastDuration()
            val playbackTime = playbackTracker.getPlaybackTime(id)
            val remainingTime = totalDuration - playbackTime
            if (remainingTime < 1000) {
                playbackTracker.updatePausedAtPlaybackTime(id, 0, 0f)
            } else {
                playbackTracker.pausePlayback(id)
            }
        }
    }
}
