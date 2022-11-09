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
import androidx.annotation.MainThread
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.duration
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.Listener
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.State
import im.vector.app.features.voicebroadcast.listening.usecase.GetLiveVoiceBroadcastChunksUseCase
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.sequence
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceBroadcastPlayerImpl @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val getVoiceBroadcastUseCase: GetVoiceBroadcastUseCase,
        private val getLiveVoiceBroadcastChunksUseCase: GetLiveVoiceBroadcastChunksUseCase
) : VoiceBroadcastPlayer {

    private val session
        get() = sessionHolder.getActiveSession()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var voiceBroadcastStateJob: Job? = null

    private val mediaPlayerListener = MediaPlayerListener()

    private var currentMediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    private var currentSequence: Int? = null

    private var fetchPlaylistJob: Job? = null
    private var playlist = emptyList<PlaylistItem>()

    private var isLive: Boolean = false

    override var currentVoiceBroadcastId: String? = null

    override var playingState = State.IDLE
        @MainThread
        set(value) {
            Timber.w("## VoiceBroadcastPlayer state: $field -> $value")
            field = value
            // Notify state change to all the listeners attached to the current voice broadcast id
            currentVoiceBroadcastId?.let { voiceBroadcastId ->
                listeners[voiceBroadcastId]?.forEach { listener -> listener.onStateChanged(value) }
            }
        }
    private var currentRoomId: String? = null

    /**
     * Map voiceBroadcastId to listeners.
     */
    private val listeners: MutableMap<String, CopyOnWriteArrayList<Listener>> = mutableMapOf()

    override fun playOrResume(roomId: String, voiceBroadcastId: String) {
        val hasChanged = currentVoiceBroadcastId != voiceBroadcastId
        when {
            hasChanged -> startPlayback(roomId, voiceBroadcastId)
            playingState == State.PAUSED -> resumePlayback()
            else -> Unit
        }
    }

    override fun pause() {
        currentMediaPlayer?.pause()
        currentVoiceBroadcastId?.let { playbackTracker.pausePlayback(it) }
        playingState = State.PAUSED
    }

    override fun stop() {
        // Stop playback
        currentMediaPlayer?.stop()
        currentVoiceBroadcastId?.let { playbackTracker.stopPlayback(it) }
        isLive = false

        // Release current player
        release(currentMediaPlayer)
        currentMediaPlayer = null

        // Release next player
        release(nextMediaPlayer)
        nextMediaPlayer = null

        // Do not observe anymore voice broadcast state changes
        voiceBroadcastStateJob?.cancel()
        voiceBroadcastStateJob = null

        // Do not fetch the playlist anymore
        fetchPlaylistJob?.cancel()
        fetchPlaylistJob = null

        // Update state
        playingState = State.IDLE

        // Clear playlist
        playlist = emptyList()
        currentSequence = null

        currentRoomId = null
        currentVoiceBroadcastId = null
    }

    override fun addListener(voiceBroadcastId: String, listener: Listener) {
        listeners[voiceBroadcastId]?.add(listener) ?: run {
            listeners[voiceBroadcastId] = CopyOnWriteArrayList<Listener>().apply { add(listener) }
        }
        if (voiceBroadcastId == currentVoiceBroadcastId) listener.onStateChanged(playingState) else listener.onStateChanged(State.IDLE)
    }

    override fun removeListener(voiceBroadcastId: String, listener: Listener) {
        listeners[voiceBroadcastId]?.remove(listener)
    }

    private fun startPlayback(roomId: String, eventId: String) {
        // Stop listening previous voice broadcast if any
        if (playingState != State.IDLE) stop()

        currentRoomId = roomId
        currentVoiceBroadcastId = eventId

        playingState = State.BUFFERING

        val voiceBroadcastState = getVoiceBroadcastUseCase.execute(roomId, eventId)?.content?.voiceBroadcastState
        isLive = voiceBroadcastState != null && voiceBroadcastState != VoiceBroadcastState.STOPPED
        fetchPlaylistAndStartPlayback(roomId, eventId)
    }

    private fun fetchPlaylistAndStartPlayback(roomId: String, voiceBroadcastId: String) {
        fetchPlaylistJob = getLiveVoiceBroadcastChunksUseCase.execute(roomId, voiceBroadcastId)
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
                    coroutineScope.launch { nextMediaPlayer = prepareNextMediaPlayer() }
                }
            }
            State.PAUSED -> {
                if (nextMediaPlayer == null) {
                    coroutineScope.launch { nextMediaPlayer = prepareNextMediaPlayer() }
                }
            }
            State.BUFFERING -> {
                val newMediaContent = getNextAudioContent()
                if (newMediaContent != null) startPlayback()
            }
            State.IDLE -> startPlayback()
        }
    }

    private fun startPlayback(sequence: Int? = null, position: Int = 0) {
        val playlistItem = when {
            sequence != null -> playlist.find { it.audioEvent.sequence == sequence }
            isLive -> playlist.lastOrNull()
            else -> playlist.firstOrNull()
        }
        val content = playlistItem?.audioEvent?.content ?: run { Timber.w("## VoiceBroadcastPlayer: No content to play"); return }
        val computedSequence = playlistItem.audioEvent.sequence
        coroutineScope.launch {
            try {
                currentMediaPlayer = prepareMediaPlayer(content)
                currentMediaPlayer?.start()
                if (position > 0) {
                    currentMediaPlayer?.seekTo(position)
                }
                currentVoiceBroadcastId?.let { playbackTracker.startPlayback(it) }
                currentSequence = computedSequence
                withContext(Dispatchers.Main) { playingState = State.PLAYING }
                nextMediaPlayer = prepareNextMediaPlayer()
            } catch (failure: Throwable) {
                Timber.e(failure, "Unable to start playback")
                throw VoiceFailure.UnableToPlay(failure)
            }
        }
    }

    private fun resumePlayback() {
        currentMediaPlayer?.start()
        currentVoiceBroadcastId?.let { playbackTracker.startPlayback(it) }
        playingState = State.PLAYING
    }

    override fun seekTo(positionMillis: Int) {
        val duration = getVoiceBroadcastDuration()
        val playlistItem = playlist.lastOrNull { it.startTime <= positionMillis } ?: return
        val audioEvent = playlistItem.audioEvent
        val eventPosition = positionMillis - playlistItem.startTime

        Timber.d("## Voice Broadcast | seekTo - duration=$duration, position=$positionMillis, sequence=${audioEvent.sequence}, sequencePosition=$eventPosition")

        tryOrNull { currentMediaPlayer?.stop() }
        release(currentMediaPlayer)
        tryOrNull { nextMediaPlayer?.stop() }
        release(nextMediaPlayer)

        startPlayback(audioEvent.sequence, eventPosition)
    }

    private fun getNextAudioContent(): MessageAudioContent? {
        val nextSequence = currentSequence?.plus(1)
                ?: playlist.lastOrNull()?.audioEvent?.sequence
                ?: 1
        return playlist.find { it.audioEvent.sequence == nextSequence }?.audioEvent?.content
    }

    private suspend fun prepareNextMediaPlayer(): MediaPlayer? {
        val nextContent = getNextAudioContent() ?: return null
        return prepareMediaPlayer(nextContent)
    }

    private suspend fun prepareMediaPlayer(messageAudioContent: MessageAudioContent): MediaPlayer {
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
                setOnCompletionListener(mediaPlayerListener)
                prepare()
            }
        }
    }

    private fun release(mp: MediaPlayer?) {
        mp?.apply {
            release()
            setOnInfoListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
        }
    }

    private inner class MediaPlayerListener :
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener {

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            when (what) {
                MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT -> {
                    release(currentMediaPlayer)
                    currentMediaPlayer = mp
                    currentSequence = currentSequence?.plus(1)
                    coroutineScope.launch { nextMediaPlayer = prepareNextMediaPlayer() }
                }
            }
            return false
        }

        override fun onPrepared(mp: MediaPlayer) {
            when (mp) {
                currentMediaPlayer -> {
                    nextMediaPlayer?.let { mp.setNextMediaPlayer(it) }
                }
                nextMediaPlayer -> {
                    tryOrNull { currentMediaPlayer?.setNextMediaPlayer(mp) }
                }
            }
        }

        override fun onCompletion(mp: MediaPlayer) {
            if (nextMediaPlayer != null) return
            val roomId = currentRoomId ?: return
            val voiceBroadcastId = currentVoiceBroadcastId ?: return
            val voiceBroadcastEventContent = getVoiceBroadcastUseCase.execute(roomId, voiceBroadcastId)?.content ?: return
            isLive = voiceBroadcastEventContent.voiceBroadcastState != null && voiceBroadcastEventContent.voiceBroadcastState != VoiceBroadcastState.STOPPED

            if (!isLive && voiceBroadcastEventContent.lastChunkSequence == currentSequence) {
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
}
