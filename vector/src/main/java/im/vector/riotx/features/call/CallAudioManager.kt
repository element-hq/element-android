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

package im.vector.riotx.features.call

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import im.vector.matrix.android.api.session.call.MxCall
import timber.log.Timber

class CallAudioManager(
        val applicationContext: Context
) {

    enum class SoundDevice {
        PHONE,
        SPEAKER
    }

    private val audioManager: AudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var savedIsSpeakerPhoneOn = false
    private var savedIsMicrophoneMute = false
    private var savedAudioMode = AudioManager.MODE_INVALID

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->

        // Called on the listener to notify if the audio focus for this listener has been changed.
        // The |focusChange| value indicates whether the focus was gained, whether the focus was lost,
        // and whether that loss is transient, or whether the new focus holder will hold it for an
        // unknown amount of time.
        Timber.v("## VOIP: Audio focus change $focusChange")
    }

    fun startForCall(mxCall: MxCall) {
        Timber.v("## VOIP: AudioManager startForCall ${mxCall.callId}")
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn
        savedIsMicrophoneMute = audioManager.isMicrophoneMute
        savedAudioMode = audioManager.mode

        // Request audio playout focus (without ducking) and install listener for changes in focus.

        // Remove the deprecation forces us to use 2 different method depending on API level
        @Suppress("DEPRECATION") val result = audioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.d("## VOIP Audio focus request granted for VOICE_CALL streams")
        } else {
            Timber.d("## VOIP Audio focus request failed")
        }

        // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
        // required to be in this mode when playout and/or recording starts for
        // best possible VoIP performance.
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // Always disable microphone mute during a WebRTC call.
        setMicrophoneMute(false)

        // TODO check if there are headsets?
        if (mxCall.isVideoCall) {
            setSpeakerphoneOn(true)
        } else {
            setSpeakerphoneOn(false)
        }
    }

    fun stop() {
        Timber.v("## VOIP: AudioManager stopCall")

        // Restore previously stored audio states.
        setSpeakerphoneOn(savedIsSpeakerPhoneOn)
        setMicrophoneMute(savedIsMicrophoneMute)
        audioManager.mode = savedAudioMode

        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    fun getCurrentSoundDevice() : SoundDevice {
        if (audioManager.isSpeakerphoneOn) {
            return SoundDevice.SPEAKER
        } else {
            return SoundDevice.PHONE
        }
    }

    fun setCurrentSoundDevice(device: SoundDevice)  {
        when (device) {
            SoundDevice.PHONE   -> setSpeakerphoneOn(false)
            SoundDevice.SPEAKER -> setSpeakerphoneOn(true)
        }
    }

    /** Sets the speaker phone mode.  */
    private fun setSpeakerphoneOn(on: Boolean) {
        Timber.v("## VOIP: AudioManager setSpeakerphoneOn $on")
        val wasOn = audioManager.isSpeakerphoneOn
        if (wasOn == on) {
            return
        }
        audioManager.isSpeakerphoneOn = on
    }

    /** Sets the microphone mute state.  */
    private fun setMicrophoneMute(on: Boolean) {
        Timber.v("## VOIP: AudioManager setMicrophoneMute $on")
        val wasMuted = audioManager.isMicrophoneMute
        if (wasMuted == on) {
            return
        }
        audioManager.isMicrophoneMute = on

        audioManager.isMusicActive
    }

    /** true if the device has a telephony radio with data
     * communication support.   */
    private fun isThisPhone(): Boolean {
        return applicationContext.packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY)
    }
}
