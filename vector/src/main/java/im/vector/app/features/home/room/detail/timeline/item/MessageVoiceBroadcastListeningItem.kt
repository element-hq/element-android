/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.text.format.DateUtils
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.room.detail.RoomDetailAction.VoiceBroadcastAction
import im.vector.app.features.home.room.detail.timeline.helper.AudioMessagePlaybackTracker.Listener.State
import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.views.VoiceBroadcastBufferingView
import im.vector.app.features.voicebroadcast.views.VoiceBroadcastMetadataView
import im.vector.lib.strings.CommonStrings

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

        playbackTracker.track(voiceBroadcast.voiceBroadcastId) { playbackState ->
            renderBackwardForwardButtons(holder, playbackState)
            renderPlaybackError(holder, playbackState)
            renderLiveIndicator(holder)
            if (!isUserSeeking) {
                holder.seekBar.progress = playbackTracker.getPlaybackTime(voiceBroadcast.voiceBroadcastId) ?: 0
            }
        }

        bindSeekBar(holder)
        bindButtons(holder)
    }

    private fun bindButtons(holder: Holder) {
        with(holder) {
            playPauseButton.setOnClickListener {
                if (player.currentVoiceBroadcast == voiceBroadcast) {
                    when (player.playingState) {
                        VoiceBroadcastPlayer.State.Playing,
                        VoiceBroadcastPlayer.State.Buffering -> callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.Pause)
                        VoiceBroadcastPlayer.State.Paused,
                        is VoiceBroadcastPlayer.State.Error,
                        VoiceBroadcastPlayer.State.Idle -> callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.PlayOrResume(voiceBroadcast))
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
            listenersCountMetadata.isVisible = false
        }
    }

    override fun renderLiveIndicator(holder: Holder) {
        when {
            voiceBroadcastState == null || voiceBroadcastState == VoiceBroadcastState.STOPPED -> renderNoLiveIndicator(holder)
            voiceBroadcastState == VoiceBroadcastState.PAUSED -> renderPausedLiveIndicator(holder)
            else -> renderPlayingLiveIndicator(holder)
        }
    }

    private fun renderPlayingState(holder: Holder, state: VoiceBroadcastPlayer.State) {
        with(holder) {
            bufferingView.isVisible = state == VoiceBroadcastPlayer.State.Buffering
            voiceBroadcastMetadata.isVisible = state != VoiceBroadcastPlayer.State.Buffering

            when (state) {
                VoiceBroadcastPlayer.State.Playing,
                VoiceBroadcastPlayer.State.Buffering -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_pause)
                    playPauseButton.contentDescription = view.resources.getString(CommonStrings.a11y_pause_voice_broadcast)
                }
                is VoiceBroadcastPlayer.State.Error,
                VoiceBroadcastPlayer.State.Idle,
                VoiceBroadcastPlayer.State.Paused -> {
                    playPauseButton.setImageResource(R.drawable.ic_play_pause_play)
                    playPauseButton.contentDescription = view.resources.getString(CommonStrings.a11y_play_voice_broadcast)
                }
            }

            renderLiveIndicator(holder)
        }
    }

    private fun renderPlaybackError(holder: Holder, playbackState: State) {
        with(holder) {
            when {
                playbackState is State.Error -> {
                    controlsGroup.isVisible = false
                    errorView.setTextOrHide(errorFormatter.toHumanReadable(playbackState.failure))
                }
                playbackState is State.Idle && hasUnableToDecryptEvent -> {
                    controlsGroup.isVisible = false
                    errorView.setTextOrHide(errorFormatter.toHumanReadable(VoiceBroadcastFailure.ListeningError.UnableToDecrypt))
                }
                else -> {
                    errorView.isVisible = false
                    controlsGroup.isVisible = true
                }
            }
        }
    }

    private fun bindSeekBar(holder: Holder) {
        with(holder) {
            remainingTimeView.text = formatRemainingTime(duration)
            elapsedTimeView.text = formatPlaybackTime(0)
            seekBar.max = duration
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    remainingTimeView.text = formatRemainingTime(duration - progress)
                    elapsedTimeView.text = formatPlaybackTime(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    callback?.onTimelineItemAction(VoiceBroadcastAction.Listening.SeekTo(voiceBroadcast, seekBar.progress, duration))
                    isUserSeeking = false
                }
            })
        }
    }

    private fun renderBackwardForwardButtons(holder: Holder, playbackState: State) {
        val isPlayingOrPaused = playbackState is State.Playing || playbackState is State.Paused
        val playbackTime = playbackTracker.getPlaybackTime(voiceBroadcast.voiceBroadcastId) ?: 0
        val canBackward = isPlayingOrPaused && playbackTime > 0
        val canForward = isPlayingOrPaused && playbackTime < duration
        holder.fastBackwardButton.isInvisible = !canBackward
        holder.fastForwardButton.isInvisible = !canForward
    }

    private fun formatPlaybackTime(time: Int) = DateUtils.formatElapsedTime((time / 1000).toLong())
    private fun formatRemainingTime(time: Int) = if (time < 1000) formatPlaybackTime(time) else String.format("-%s", formatPlaybackTime(time))

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
        val bufferingView by bind<VoiceBroadcastBufferingView>(R.id.bufferingMetadata)
        val fastBackwardButton by bind<ImageButton>(R.id.fastBackwardButton)
        val fastForwardButton by bind<ImageButton>(R.id.fastForwardButton)
        val seekBar by bind<SeekBar>(R.id.seekBar)
        val remainingTimeView by bind<TextView>(R.id.remainingTime)
        val elapsedTimeView by bind<TextView>(R.id.elapsedTime)
        val broadcasterNameMetadata by bind<VoiceBroadcastMetadataView>(R.id.broadcasterNameMetadata)
        val voiceBroadcastMetadata by bind<VoiceBroadcastMetadataView>(R.id.voiceBroadcastMetadata)
        val listenersCountMetadata by bind<VoiceBroadcastMetadataView>(R.id.listenersCountMetadata)
        val errorView by bind<TextView>(R.id.errorView)
        val controlsGroup by bind<Group>(R.id.controlsGroup)
    }

    companion object {
        private val STUB_ID = R.id.messageVoiceBroadcastListeningStub
    }
}
