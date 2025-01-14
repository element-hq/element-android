/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.listening

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.MainThread
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.onFirst
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure
import im.vector.app.features.voicebroadcast.isLive
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.Listener
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer.State
import im.vector.app.features.voicebroadcast.listening.usecase.GetLiveVoiceBroadcastChunksUseCase
import im.vector.app.features.voicebroadcast.model.VoiceBroadcast
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventLiveUseCase
import im.vector.lib.core.utils.timer.CountUpTimer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
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
        private set(value) {
            Timber.d("## Voice Broadcast | currentMediaPlayer changed: old=${field.hashCode()}, new=${value.hashCode()}")
            field = value
        }
    private var nextMediaPlayer: MediaPlayer? = null
        private set(value) {
            Timber.d("## Voice Broadcast | nextMediaPlayer changed:    old=${field.hashCode()}, new=${value.hashCode()}")
            field = value
        }

    private var prepareCurrentPlayerJob: Job? = null
        set(value) {
            if (field?.isActive.orFalse()) field?.cancel()
            field = value
        }
    private var prepareNextPlayerJob: Job? = null
        set(value) {
            if (field?.isActive.orFalse()) field?.cancel()
            field = value
        }

    private val isPreparingCurrentPlayer: Boolean get() = prepareCurrentPlayerJob?.isActive.orFalse()
    private val isPreparingNextPlayer: Boolean get() = prepareNextPlayerJob?.isActive.orFalse()

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

    override var playingState: State = State.Idle
        @MainThread
        set(value) {
            if (field != value) {
                Timber.d("## Voice Broadcast | playingState: ${field::class.java.simpleName} -> ${value::class.java.simpleName}")
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
            playingState == State.Paused -> resumePlayback()
            else -> Unit
        }
    }

    override fun pause() {
        pausePlayback()
    }

    override fun stop() {
        // Update state
        playingState = State.Idle

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
        listener.onPlayingStateChanged(if (voiceBroadcast == currentVoiceBroadcast) playingState else State.Idle)
        listener.onLiveModeChanged(voiceBroadcast == currentVoiceBroadcast)
    }

    override fun removeListener(voiceBroadcast: VoiceBroadcast, listener: Listener) {
        listeners[voiceBroadcast.voiceBroadcastId]?.remove(listener)
    }

    private fun startPlayback(voiceBroadcast: VoiceBroadcast) {
        // Stop listening previous voice broadcast if any
        if (playingState != State.Idle) stop()

        currentVoiceBroadcast = voiceBroadcast

        playingState = State.Buffering

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
                .onEach { events ->
                    if (events.any { it.getClearType() == EventType.ENCRYPTED }) {
                        playingState = State.Error(VoiceBroadcastFailure.ListeningError.UnableToDecrypt)
                    } else {
                        playlist.setItems(events.mapNotNull { it.asMessageAudioEvent() })
                        onPlaylistUpdated()
                    }
                }
                .launchIn(sessionScope)
    }

    private fun onPlaylistUpdated() {
        if (isPreparingCurrentPlayer || isPreparingNextPlayer) return
        when (playingState) {
            State.Playing,
            State.Paused -> {
                if (nextMediaPlayer == null) {
                    prepareNextMediaPlayer()
                }
            }
            State.Buffering -> {
                val savedPosition = currentVoiceBroadcast?.let { playbackTracker.getPlaybackTime(it.voiceBroadcastId) }
                when {
                    // resume playback from the next sequence item
                    playlist.currentSequence != null -> playlist.getNextItem()?.let { startPlayback(it.startTime) }
                    // resume playback from the saved position, if any
                    savedPosition != null -> startPlayback(savedPosition)
                    // live listening, jump to the last item
                    isLiveListening -> playlist.lastOrNull()?.let { startPlayback(it.startTime) }
                    // start playback from the beginning
                    else -> startPlayback(0)
                }
            }
            is State.Error -> Unit
            State.Idle -> Unit // Should not happen
        }
    }

    private fun startPlayback(playbackPosition: Int) {
        stopPlayer()
        playingState = State.Buffering

        val playlistItem = playlist.findByPosition(playbackPosition) ?: run {
            Timber.w("## Voice Broadcast | No content to play at position $playbackPosition"); stop(); return
        }
        val sequence = playlistItem.sequence ?: run {
            Timber.w("## Voice Broadcast | Playlist item has no sequence"); stop(); return
        }

        currentVoiceBroadcast?.let {
            val percentage = tryOrNull { playbackPosition.toFloat() / playlist.duration } ?: 0f
            playbackTracker.updatePausedAtPlaybackTime(it.voiceBroadcastId, playbackPosition, percentage)
        }

        prepareCurrentPlayerJob = sessionScope.launch {
            try {
                val mp = prepareMediaPlayer(playlistItem.audioEvent.content)

                // Take the difference between the duration given from the media player and the duration given from the chunk event
                // If the offset is smaller than 500ms, we consider there is no offset to keep the normal behaviour
                val offset = (mp.duration - playlistItem.duration).takeUnless { it < 500 }?.coerceAtLeast(0) ?: 0
                val sequencePosition = offset + (playbackPosition - playlistItem.startTime)

                playlist.currentSequence = sequence - 1 // will be incremented in onNextMediaPlayerStarted
                mp.start()
                if (sequencePosition > 0) {
                    mp.seekTo(sequencePosition)
                }

                onNextMediaPlayerStarted(mp)
            } catch (failure: VoiceBroadcastFailure.ListeningError) {
                if (failure.cause !is CancellationException) {
                    playingState = State.Error(failure)
                }
            }
        }
    }

    private fun pausePlayback() {
        playingState = State.Paused // This will trigger a playing state update and save the current position
        if (currentMediaPlayer != null) {
            currentMediaPlayer?.pause()
        } else {
            stopPlayer()
        }
    }

    private fun resumePlayback() {
        if (currentMediaPlayer != null) {
            playingState = State.Playing
            currentMediaPlayer?.start()
        } else {
            val savedPosition = currentVoiceBroadcast?.let { playbackTracker.getPlaybackTime(it.voiceBroadcastId) } ?: 0
            startPlayback(savedPosition)
        }
    }

    override fun seekTo(voiceBroadcast: VoiceBroadcast, positionMillis: Int, duration: Int) {
        when {
            voiceBroadcast != currentVoiceBroadcast -> {
                playbackTracker.updatePausedAtPlaybackTime(voiceBroadcast.voiceBroadcastId, positionMillis, positionMillis.toFloat() / duration)
            }
            playingState == State.Playing || playingState == State.Buffering -> {
                startPlayback(positionMillis)
            }
            playingState == State.Idle || playingState == State.Paused -> {
                stopPlayer()
                playbackTracker.updatePausedAtPlaybackTime(voiceBroadcast.voiceBroadcastId, positionMillis, positionMillis.toFloat() / duration)
            }
        }
    }

    private fun prepareNextMediaPlayer() {
        val nextItem = playlist.getNextItem()
        if (!isPreparingNextPlayer && nextMediaPlayer == null && nextItem != null) {
            prepareNextPlayerJob = sessionScope.launch {
                try {
                    val mp = prepareMediaPlayer(nextItem.audioEvent.content)
                    nextMediaPlayer = mp
                    when (playingState) {
                        State.Playing,
                        State.Paused -> {
                            currentMediaPlayer?.setNextMediaPlayer(mp)
                        }
                        State.Buffering -> {
                            mp.start()
                            onNextMediaPlayerStarted(mp)
                        }
                        is State.Error,
                        State.Idle -> stopPlayer()
                    }
                } catch (failure: VoiceBroadcastFailure.ListeningError) {
                    // Do not change the playingState if the current player is still valid,
                    // the error will be thrown again when switching to the next player
                    if (failure.cause !is CancellationException && (playingState == State.Buffering || tryOrNull { currentMediaPlayer?.isPlaying } != true)) {
                        playingState = State.Error(failure)
                    }
                }
            }
        }
    }

    /**
     * Create and prepare a [MediaPlayer] instance for the given [messageAudioContent].
     * This methods takes care of downloading the audio file and returns the player when it is ready to use.
     *
     * Do not forget to release the resulting player when you don't need it anymore, in case you cancel the job related to this method, the player will be
     * automatically released.
     */
    private suspend fun prepareMediaPlayer(messageAudioContent: MessageAudioContent): MediaPlayer {
        // Download can fail
        val audioFile = try {
            session.fileService().downloadFile(messageAudioContent)
        } catch (failure: Throwable) {
            Timber.e(failure, "Voice Broadcast | Download has failed: $failure")
            throw VoiceBroadcastFailure.ListeningError.PrepareMediaPlayerError(failure)
        }

        val latch = CompletableDeferred<MediaPlayer>()
        val mp = MediaPlayer()
        return try {
            mp.apply {
                setOnErrorListener { mp, what, extra ->
                    mediaPlayerListener.onError(mp, what, extra)
                    latch.completeExceptionally(VoiceBroadcastFailure.ListeningError.PrepareMediaPlayerError())
                }
                setAudioAttributes(
                        AudioAttributes.Builder()
                                // Do not use CONTENT_TYPE_SPEECH / USAGE_VOICE_COMMUNICATION because we want to play loud here
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                )
                audioFile.inputStream().use { fis -> setDataSource(fis.fd) }
                setOnInfoListener(mediaPlayerListener)
                setOnPreparedListener(latch::complete)
                setOnCompletionListener(mediaPlayerListener)
                prepareAsync()
            }
            latch.await()
        } catch (e: CancellationException) {
            mp.release()
            throw e
        }
    }

    private fun stopPlayer() {
        tryOrNull { currentMediaPlayer?.stop() }
        playbackTicker.stopPlaybackTicker()

        currentMediaPlayer?.release()
        currentMediaPlayer = null

        nextMediaPlayer?.release()
        nextMediaPlayer = null

        prepareCurrentPlayerJob = null
        prepareNextPlayerJob = null
    }

    private fun onPlayingStateChanged(playingState: State) {
        // Update live playback flag
        updateLiveListeningMode()

        currentVoiceBroadcast?.voiceBroadcastId?.let { voiceBroadcastId ->
            // Start or stop playback ticker
            when (playingState) {
                State.Playing -> playbackTicker.startPlaybackTicker(voiceBroadcastId)
                State.Paused,
                State.Buffering,
                is State.Error,
                State.Idle -> playbackTicker.stopPlaybackTicker()
            }

            // Notify playback tracker about error
            if (playingState is State.Error) {
                playbackTracker.onError(voiceBroadcastId, playingState.failure)
            }

            // Notify state change to all the listeners attached to the current voice broadcast id
            listeners[voiceBroadcastId]?.forEach { listener -> listener.onPlayingStateChanged(playingState) }
        }
    }

    /**
     * Update the live listening state according to:
     * - the voice broadcast state (started/paused/resumed/stopped),
     * - the playing state (IDLE, PLAYING, PAUSED, BUFFERING).
     */
    private fun updateLiveListeningMode() {
        val isLiveVoiceBroadcast = mostRecentVoiceBroadcastEvent?.isLive.orFalse()
        val isPlaying = playingState == State.Playing || playingState == State.Buffering
        isLiveListening = isLiveVoiceBroadcast && isPlaying
    }

    private fun onLiveListeningChanged(isLiveListening: Boolean) {
        // Live has ended and last chunk has been reached, we can stop the playback
        val hasReachedLastChunk = playlist.currentSequence == mostRecentVoiceBroadcastEvent?.content?.lastChunkSequence
        if (!isLiveListening && playingState == State.Buffering && hasReachedLastChunk) {
            stop()
        }
    }

    private fun onNextMediaPlayerStarted(mp: MediaPlayer) {
        playingState = State.Playing
        playlist.currentSequence = playlist.currentSequence?.inc()
        currentMediaPlayer = mp
        nextMediaPlayer = null
        prepareNextMediaPlayer()
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
            // Release media player as soon as it completed
            mp.release()
            if (currentMediaPlayer == mp) {
                currentMediaPlayer = null
            } else {
                Timber.w(
                        "## Voice Broadcast | onCompletion: The media player which has completed mismatches the current media player instance.\n" +
                                "currentMediaPlayer=${currentMediaPlayer.hashCode()}, mp=${mp.hashCode()}"
                )
            }

            // Next media player is already attached to this player and will start playing automatically
            if (nextMediaPlayer != null) return

            val currentSequence = playlist.currentSequence ?: 0
            val lastChunkSequence = mostRecentVoiceBroadcastEvent?.content?.lastChunkSequence ?: 0
            val hasEnded = !isLiveListening && currentSequence >= lastChunkSequence
            if (hasEnded) {
                // We'll not receive new chunks anymore so we can stop the live listening
                stop()
            } else {
                playingState = State.Buffering
                prepareNextMediaPlayer()
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            Timber.w("## Voice Broadcast | onError: what=$what, extra=$extra")
            // Do not change the playingState if the current player is still valid,
            // the error will be thrown again when switching to the next player
            if (playingState == State.Buffering || tryOrNull { currentMediaPlayer?.isPlaying } != true) {
                playingState = State.Error(VoiceBroadcastFailure.ListeningError.UnableToPlay(what, extra))
            }
            return true
        }
    }

    private inner class PlaybackTicker(
            private var playbackTicker: CountUpTimer? = null,
    ) {

        fun startPlaybackTicker(id: String) {
            playbackTicker?.stop()
            playbackTicker = CountUpTimer(intervalInMs = 50L).apply {
                tickListener = CountUpTimer.TickListener { onPlaybackTick(id, it.toInt()) }
                start(initialTime = playbackTracker.getPlaybackTime(id)?.toLong() ?: 0L)
            }
        }

        fun stopPlaybackTicker() {
            playbackTicker?.stop()
            playbackTicker?.tickListener = null
            playbackTicker = null
        }

        private fun onPlaybackTick(id: String, position: Int) {
            val percentage = tryOrNull { position.toFloat() / playlist.duration }
            when (playingState) {
                State.Playing -> {
                    if (percentage != null) {
                        playbackTracker.updatePlayingAtPlaybackTime(id, position, percentage)
                    }
                }
                State.Paused,
                State.Buffering -> {
                    if (percentage != null) {
                        playbackTracker.updatePausedAtPlaybackTime(id, position, percentage)
                    }
                }
                State.Idle -> {
                    // restart the playback time if player completed with less than 1s remaining time
                    if (percentage == null || (playlist.duration - position) < 1000) {
                        playbackTracker.stopPlaybackOrRecorder(id)
                    } else {
                        playbackTracker.updatePausedAtPlaybackTime(id, position, percentage)
                    }
                }
                is State.Error -> Unit
            }
        }
    }
}
