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
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker.Listener.State
import im.vector.app.features.voice.VoiceFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioEvent
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceBroadcastPlayer @Inject constructor(
        private val session: Session,
        private val playbackTracker: AudioMessagePlaybackTracker,
) {

    private val mediaPlayerScope = CoroutineScope(Dispatchers.IO)

    private var currentMediaPlayer: MediaPlayer? = null
    private var currentPlayingIndex: Int = -1
    private var playlist = emptyList<MessageAudioEvent>()
    private val currentVoiceBroadcastEventId
        get() = playlist.firstOrNull()?.root?.getRelationContent()?.eventId

    private val mediaPlayerListener = MediaPlayerListener()

    fun play(roomId: String, eventId: String) {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        when {
            currentVoiceBroadcastEventId != eventId -> {
                stop()
                updatePlaylist(room, eventId)
                startPlayback()
            }
            playbackTracker.getPlaybackState(eventId) is State.Playing -> pause()
            else -> resumePlayback()
        }
    }

    fun pause() {
        currentMediaPlayer?.pause()
        currentVoiceBroadcastEventId?.let { playbackTracker.pausePlayback(it) }
    }

    fun stop() {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()
        currentMediaPlayer?.setOnInfoListener(null)
        currentMediaPlayer = null
        currentVoiceBroadcastEventId?.let { playbackTracker.stopPlayback(it) }
        playlist = emptyList()
        currentPlayingIndex = -1
    }

    private fun updatePlaylist(room: Room, eventId: String) {
        val timelineEvents = room.timelineService().getTimelineEventsRelatedTo(RelationType.REFERENCE, eventId)
        val audioEvents = timelineEvents.mapNotNull { it.root.asMessageAudioEvent() }
        playlist = audioEvents.sortedBy { it.getVoiceBroadcastChunk()?.sequence?.toLong() ?: it.root.originServerTs }
    }

    private fun startPlayback() {
        val content = playlist.firstOrNull()?.content ?: run { Timber.w("## VoiceBroadcastPlayer: No content to play"); return }
        mediaPlayerScope.launch {
            try {
                currentMediaPlayer = prepareMediaPlayer(content)
                currentMediaPlayer?.start()
                currentPlayingIndex = 0
                currentVoiceBroadcastEventId?.let { playbackTracker.startPlayback(it) }
                prepareNextFile()
            } catch (failure: Throwable) {
                Timber.e(failure, "Unable to start playback")
                throw VoiceFailure.UnableToPlay(failure)
            }
        }
    }

    private fun resumePlayback() {
        currentMediaPlayer?.start()
        currentVoiceBroadcastEventId?.let { playbackTracker.startPlayback(it) }
    }

    private suspend fun prepareNextFile() {
        val nextContent = playlist.getOrNull(currentPlayingIndex + 1)?.content
        if (nextContent == null) {
            currentMediaPlayer?.setOnCompletionListener(mediaPlayerListener)
        } else {
            val nextMediaPlayer = prepareMediaPlayer(nextContent)
            currentMediaPlayer?.setNextMediaPlayer(nextMediaPlayer)
        }
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
                prepare()
            }
        }
    }

    inner class MediaPlayerListener : MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            when (what) {
                MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT -> {
                    currentMediaPlayer = mp
                    currentPlayingIndex++
                    mediaPlayerScope.launch { prepareNextFile() }
                }
            }
            return false
        }

        override fun onCompletion(mp: MediaPlayer) {
            // Verify that a new media has not been set in the mean time
            if (!currentMediaPlayer?.isPlaying.orFalse()) {
                stop()
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            stop()
            return true
        }
    }
}
