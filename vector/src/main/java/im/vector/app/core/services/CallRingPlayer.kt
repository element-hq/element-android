/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.services

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.content.getSystemService
import im.vector.app.R
import timber.log.Timber

class CallRingPlayerIncoming(
        context: Context
) {

    private val applicationContext = context.applicationContext
    private var r: Ringtone? = null

    fun start() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        r = RingtoneManager.getRingtone(applicationContext, notification)
        Timber.v("## VOIP Starting ringing incomming")
        r?.play()
    }

    fun stop() {
        r?.stop()
    }
}

class CallRingPlayerOutgoing(
        context: Context
) {

    private val applicationContext = context.applicationContext

    private var player: MediaPlayer? = null

    fun start() {
        val audioManager = applicationContext.getSystemService<AudioManager>()!!
        player?.release()
        player = createPlayer()

        // Check if sound is enabled
        val ringerMode = audioManager.ringerMode
        if (player != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                if (player?.isPlaying == false) {
                    player?.start()
                    Timber.v("## VOIP Starting ringing outgoing")
                } else {
                    Timber.v("## VOIP already playing")
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "## VOIP Failed to start ringing outgoing")
                player = null
            }
        } else {
            Timber.v("## VOIP Can't play $player ode $ringerMode")
        }
    }

    fun stop() {
        player?.release()
        player = null
    }

    private fun createPlayer(): MediaPlayer? {
        try {
            val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.ring)

            mediaPlayer.setOnErrorListener(MediaPlayerErrorListener())
            mediaPlayer.isLooping = true
            if (Build.VERSION.SDK_INT <= 21) {
                @Suppress("DEPRECATION")
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING)
            } else {
                mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build())
            }
            return mediaPlayer
        } catch (failure: Throwable) {
            Timber.e(failure, "Failed to create Call ring player")
            return null
        }
    }

    inner class MediaPlayerErrorListener : MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            Timber.w("onError($mp, $what, $extra")
            player = null
            return false
        }
    }
}
