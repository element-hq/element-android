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

package im.vector.app.features.home.room.detail.timeline.item

import android.text.format.DateUtils
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.room.detail.RoomDetailAction.VoiceBroadcastAction
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker.Listener.State
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.views.VoiceBroadcastMetadataView

@EpoxyModelClass
abstract class MessageVoiceBroadcastListeningItem : AbsMessageVoiceBroadcastItem<MessageVoiceBroadcastListeningItem.Holder>() {

    private lateinit var playerListener: VoiceBroadcastPlayer.Listener
    private var isUserSeeking = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindVoiceBroadcastItem(holder)
    }

    private fun bindVoiceBroadcastItem(holder: Holder) {
        playerListener = object : VoiceBroadcastPlayer.Listener {
            override fun onPlayingStateChanged(state: VoiceBroadcastPlayer.State) {
                renderPlayingState(holder, state)
            }

            override fun onLiveModeChanged(isLive: Boolean) {
                renderLiveIndicator(holder)
            }
        }
        player.addListener(voiceBroadcast, playerListener)
        bindSeekBar(holder)
        bindButtons(holder)
    }

    private fun bindButtons(holder: Holder) {
        with(holder) {
            playPauseButton.setOnClickListener {
                if (player.currentVoiceBroadcast == voiceBroadcast) {
                    when (player.playingState) {
                        VoiceBroadcastPlayer.State.PLAYING -> callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.Pause)
                        VoiceBroadcastPlayer.State.PAUSED,
                        VoiceBroadcastPlayer.State.IDLE -> callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcast))
                        VoiceBroadcastPlayer.State.BUFFERING -> Unit
                    }
                } else {
                    callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcast))
                }
            }
            fastBackwardButton.setOnClickListener {
                val newPos = seekBar.progress.minus(30_000).coerceIn(0, duration)
                callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.SeekTo(voiceBroadcast, newPos, duration))
            }
            fastForwardButton.setOnClickListener {
                val newPos = seekBar.progress.plus(30_000).coerceIn(0, duration)
                callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.SeekTo(voiceBroadcast, newPos, duration))
            }
        }
    }

    override fun renderMetadata(holder: Holder) {
        with(holder) {
            broadcasterNameMetadata.value = recorderName
            voiceBroadcastMetadata.isVisible = true
            listenersCountMetadata.isVisible = false
        }
    }

    override fun renderLiveIndicator(holder: Holder) {
        when {
            voiceBroadcastState == null || voiceBroadcastState == VoiceBroadcastState.STOPPED -> renderNoLiveIndicator(holder)
            voiceBroadcastState == VoiceBroadcastState.PAUSED || !player.isLiveListening -> renderPausedLiveIndicator(holder)
            else -> renderPlayingLiveIndicator(holder)
        }
    }

    private fun renderPlayingState(holder: Holder, state: VoiceBroadcastPlayer.State) {
        with(holder) {
            bufferingView.isVisible = state == VoiceBroadcastPlayer.State.BUFFERING
            playPauseButton.isVisible = state != VoiceBroadcastPlayer.State.BUFFERING

            when (state) {
                VoiceBroadcastPlayer.State.PLAYING -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_pause)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_pause_voice_broadcast)
                }
                VoiceBroadcastPlayer.State.IDLE,
                VoiceBroadcastPlayer.State.PAUSED -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_play)
                    playPauseButton.contentDescription = view.resources.getString(R.string.a11y_play_voice_broadcast)
                }
                VoiceBroadcastPlayer.State.BUFFERING -> Unit
            }

            renderLiveIndicator(holder)
        }
    }

    private fun bindSeekBar(holder: Holder) {
        with(holder) {
            durationView.text = formatPlaybackTime(duration)
            seekBar.max = duration
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.SeekTo(voiceBroadcast, seekBar.progress, duration))
                    isUserSeeking = false
                }
            })
        }
        playbackTracker.track(voiceBroadcast.voiceBroadcastId) { playbackState ->
            renderBackwardForwardButtons(holder, playbackState)
            renderLiveIndicator(holder)
            if (!isUserSeeking) {
                holder.seekBar.progress = playbackTracker.getPlaybackTime(voiceBroadcast.voiceBroadcastId)
            }
        }
    }

    private fun renderBackwardForwardButtons(holder: Holder, playbackState: State) {
        val isPlayingOrPaused = playbackState is State.Playing || playbackState is State.Paused
        val playbackTime = playbackTracker.getPlaybackTime(voiceBroadcast.voiceBroadcastId)
        val canBackward = isPlayingOrPaused && playbackTime > 0
        val canForward = isPlayingOrPaused && playbackTime < duration
        holder.fastBackwardButton.isInvisible = !canBackward
        holder.fastForwardButton.isInvisible = !canForward
    }

    private fun formatPlaybackTime(time: Int) = DateUtils.formatElapsedTime((time / 1000).toLong())

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        player.removeListener(voiceBroadcast, playerListener)
        playbackTracker.untrack(voiceBroadcast.voiceBroadcastId)
        with(holder) {
            seekBar.setOnSeekBarChangeListener(null)
            playPauseButton.onClick(null)
            fastForwardButton.onClick(null)
            fastBackwardButton.onClick(null)
        }
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageVoiceBroadcastItem.Holder(STUB_ID) {
        val playPauseButton by bind<ImageButton>(R.id.playPauseButton)
        val bufferingView by bind<View>(R.id.bufferingView)
        val fastBackwardButton by bind<ImageButton>(R.id.fastBackwardButton)
        val fastForwardButton by bind<ImageButton>(R.id.fastForwardButton)
        val seekBar by bind<SeekBar>(R.id.seekBar)
        val durationView by bind<TextView>(R.id.playbackDuration)
        val broadcasterNameMetadata by bind<VoiceBroadcastMetadataView>(R.id.broadcasterNameMetadata)
        val voiceBroadcastMetadata by bind<VoiceBroadcastMetadataView>(R.id.voiceBroadcastMetadata)
        val listenersCountMetadata by bind<VoiceBroadcastMetadataView>(R.id.listenersCountMetadata)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastListeningStub
    }
}
