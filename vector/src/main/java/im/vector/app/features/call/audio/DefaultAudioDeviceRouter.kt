/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.audio

import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import timber.log.Timber

class DefaultAudioDeviceRouter(
        private val audioManager: AudioManager,
        private val callAudioManager: CallAudioManager
) : CallAudioManager.AudioDeviceRouter, AudioManager.OnAudioFocusChangeListener {

    private var audioFocusLost = false

    private var focusRequestCompat: AudioFocusRequestCompat? = null

    override fun setAudioRoute(device: CallAudioManager.Device) {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = device is CallAudioManager.Device.Speaker
        setBluetoothAudioRoute(device is CallAudioManager.Device.WirelessHeadset)
    }

    override fun setMode(mode: CallAudioManager.Mode): Boolean {
        if (mode === CallAudioManager.Mode.DEFAULT) {
            audioFocusLost = false
            audioManager.mode = AudioManager.MODE_NORMAL
            focusRequestCompat?.also {
                AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
            }
            focusRequestCompat = null
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            setBluetoothAudioRoute(false)
            return true
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false

        val audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                        AudioAttributesCompat.Builder()
                                .setUsage(AudioAttributesCompat.USAGE_VOICE_COMMUNICATION)
                                .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                                .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
                .also {
                    focusRequestCompat = it
                }

        val gotFocus = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)
        if (gotFocus == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Timber.w(" Audio focus request failed")
            return false
        }
        return true
    }

    /**
     * Helper method to set the output route to a Bluetooth device.
     *
     * @param enabled true if Bluetooth should use used, false otherwise.
     */
    private fun setBluetoothAudioRoute(enabled: Boolean) {
        if (enabled) {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        } else {
            audioManager.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
        }
    }

    /**
     * [AudioManager.OnAudioFocusChangeListener] interface method. Called
     * when the audio focus of the system is updated.
     *
     * @param focusChange - The type of focus change.
     */
    override fun onAudioFocusChange(focusChange: Int) {
        callAudioManager.runInAudioThread {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Timber.d(" Audio focus gained")
                    if (audioFocusLost) {
                        callAudioManager.resetAudioRoute()
                    }
                    audioFocusLost = false
                }
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Timber.d(" Audio focus lost")
                    audioFocusLost = true
                }
            }
        }
    }
}
