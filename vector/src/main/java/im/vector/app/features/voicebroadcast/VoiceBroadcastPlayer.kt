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

package im.vector.app.features.voicebroadcast

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.MainThread
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceBroadcastPlayer @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val playbackTracker: AudioMessagePlaybackTracker,
        private val getVoiceBroadcastUseCase: GetVoiceBroadcastUseCase,
) {
    private val session
        get() = sessionHolder.getActiveSession()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var voiceBroadcastStateJob: Job? = null
    private var currentTimeline: Timeline? = null
        set(value) {
            field?.removeAllListeners()
            field?.dispose()
            field = value
        }

    private val mediaPlayerListener = MediaPlayerListener()
    private var timelineListener: TimelineListener? = null

    private var currentMediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
        set(value) {
            field = value
            currentMediaPlayer?.setNextMediaPlayer(value)
        }
    private var currentSequence: Int? = null

    private var playlist = emptyList<MessageAudioEvent>()
    var currentVoiceBroadcastId: String? = null

    private var state: State = State.IDLE
        @MainThread
        set(value) {
            Timber.w("## VoiceBroadcastPlayer state: $field -> $value")
            field = value
            listeners.forEach { it.onStateChanged(value) }
        }
    private var currentRoomId: String? = null
    private var listeners = CopyOnWriteArrayList<Listener>()

    fun playOrResume(roomId: String, eventId: String) {
        val hasChanged = currentVoiceBroadcastId != eventId
        when {
            hasChanged -> startPlayback(roomId, eventId)
            state == State.PAUSED -> resumePlayback()
            else -> Unit
        }
    }

    fun pause() {
        currentMediaPlayer?.pause()
        currentVoiceBroadcastId?.let { playbackTracker.pausePlayback(it) }
        state = State.PAUSED
    }

    fun stop() {
        // Stop playback
        currentMediaPlayer?.stop()
        currentVoiceBroadcastId?.let { playbackTracker.stopPlayback(it) }

        // Release current player
        release(currentMediaPlayer)
        currentMediaPlayer = null

        // Release next player
        release(nextMediaPlayer)
        nextMediaPlayer = null

        // Do not observe anymore voice broadcast state changes
        voiceBroadcastStateJob?.cancel()
        voiceBroadcastStateJob = null

        // In case of live broadcast, stop observing new chunks
        currentTimeline = null
        timelineListener = null

        // Update state
        state = State.IDLE

        // Clear playlist
        playlist = emptyList()
        currentSequence = null
        currentRoomId = null
        currentVoiceBroadcastId = null
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.onStateChanged(state)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun startPlayback(roomId: String, eventId: String) {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")
        // Stop listening previous voice broadcast if any
        if (state != State.IDLE) stop()

        currentRoomId = roomId
        currentVoiceBroadcastId = eventId

        state = State.BUFFERING

        val voiceBroadcastState = getVoiceBroadcastUseCase.execute(roomId, eventId)?.content?.voiceBroadcastState
        if (voiceBroadcastState == VoiceBroadcastState.STOPPED) {
            // Get static playlist
            updatePlaylist(getExistingChunks(room, eventId))
            startPlayback(false)
        } else {
            playLiveVoiceBroadcast(room, eventId)
        }
    }

    private fun startPlayback(isLive: Boolean) {
        val event = if (isLive) playlist.lastOrNull() else playlist.firstOrNull()
        val content = event?.content ?: run { Timber.w("## VoiceBroadcastPlayer: No content to play"); return }
        val sequence = event.getVoiceBroadcastChunk()?.sequence
        coroutineScope.launch {
            try {
                currentMediaPlayer = prepareMediaPlayer(content)
                currentMediaPlayer?.start()
                currentVoiceBroadcastId?.let { playbackTracker.startPlayback(it) }
                currentSequence = sequence
                withContext(Dispatchers.Main) { state = State.PLAYING }
                nextMediaPlayer = prepareNextMediaPlayer()
            } catch (failure: Throwable) {
                Timber.e(failure, "Unable to start playback")
                throw VoiceFailure.UnableToPlay(failure)
            }
        }
    }

    private fun playLiveVoiceBroadcast(room: Room, eventId: String) {
        room.timelineService().getTimelineEvent(eventId)?.root?.asVoiceBroadcastEvent() ?: error("Cannot retrieve voice broadcast $eventId")
        updatePlaylist(getExistingChunks(room, eventId))
        startPlayback(true)
        observeIncomingEvents(room, eventId)
    }

    private fun getExistingChunks(room: Room, eventId: String): List<MessageAudioEvent> {
        return room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, eventId)
                .mapNotNull { it.root.asMessageAudioEvent() }
                .filter { it.isVoiceBroadcast() }
    }

    private fun observeIncomingEvents(room: Room, eventId: String) {
        currentTimeline = room.timelineService().createTimeline(null, TimelineSettings(5)).also { timeline ->
            timelineListener = TimelineListener(eventId).also { timeline.addListener(it) }
            timeline.start()
        }
    }

    private fun resumePlayback() {
        currentMediaPlayer?.start()
        currentVoiceBroadcastId?.let { playbackTracker.startPlayback(it) }
        state = State.PLAYING
    }

    private fun updatePlaylist(playlist: List<MessageAudioEvent>) {
        this.playlist = playlist.sortedBy { it.getVoiceBroadcastChunk()?.sequence?.toLong() ?: it.root.originServerTs }
    }

    private fun getNextAudioContent(): MessageAudioContent? {
        val nextSequence = currentSequence?.plus(1)
                ?: timelineListener?.let { playlist.lastOrNull()?.sequence }
                ?: 1
        return playlist.find { it.getVoiceBroadcastChunk()?.sequence == nextSequence }?.content
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

    private inner class TimelineListener(private val voiceBroadcastId: String) : Timeline.Listener {
        override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
            val currentSequences = playlist.map { it.sequence }
            val newChunks = snapshot
                    .mapNotNull { timelineEvent ->
                        timelineEvent.root.asMessageAudioEvent()
                                ?.takeIf { it.isVoiceBroadcast() && it.getVoiceBroadcastEventId() == voiceBroadcastId && it.sequence !in currentSequences }
                    }
            if (newChunks.isEmpty()) return
            updatePlaylist(playlist + newChunks)

            when (state) {
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
                    if (newMediaContent != null) startPlayback(true)
                }
                State.IDLE -> startPlayback(true)
            }
        }
    }

    private inner class MediaPlayerListener : MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

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

        override fun onCompletion(mp: MediaPlayer) {
            if (nextMediaPlayer != null) return
            val roomId = currentRoomId ?: return
            val voiceBroadcastId = currentVoiceBroadcastId ?: return
            val voiceBroadcastEventContent = getVoiceBroadcastUseCase.execute(roomId, voiceBroadcastId)?.content ?: return
            val isLive = voiceBroadcastEventContent.voiceBroadcastState != null && voiceBroadcastEventContent.voiceBroadcastState != VoiceBroadcastState.STOPPED

            if (!isLive && voiceBroadcastEventContent.lastChunkSequence == currentSequence) {
                // We'll not receive new chunks anymore so we can stop the live listening
                stop()
            } else {
                state = State.BUFFERING
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            stop()
            return true
        }
    }

    enum class State {
        PLAYING,
        PAUSED,
        BUFFERING,
        IDLE
    }

    fun interface Listener {
        fun onStateChanged(state: State)
    }
}
