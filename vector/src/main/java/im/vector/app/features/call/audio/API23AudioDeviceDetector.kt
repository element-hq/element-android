/*
 * Copyright (c) 2021 New Vector Ltd
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
package im.vector.app.features.call.audio

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.M)
internal class API23AudioDeviceDetector(private val audioManager: AudioManager,
                                        private val callAudioManager: CallAudioManager
) : CallAudioManager.AudioDeviceDetector {

    private val onAudioDeviceChangeRunner = Runnable {
        val devices: MutableSet<CallAudioManager.Device> = HashSet()
        val deviceInfos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (info in deviceInfos) {
            when (info.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> devices.add(CallAudioManager.Device.WirelessHeadset(info.productName.toString()))
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> devices.add(CallAudioManager.Device.Phone)
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> devices.add(CallAudioManager.Device.Speaker)
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, TYPE_USB_HEADSET -> devices.add(CallAudioManager.Device.Headset)
            }
        }
        callAudioManager.replaceDevices(devices)
        Timber.i(" Available audio devices: $devices")
        callAudioManager.updateAudioRoute()
    }
    private val audioDeviceCallback: AudioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(
                addedDevices: Array<AudioDeviceInfo>) {
            Timber.d(" Audio devices added")
            onAudioDeviceChange()
        }

        override fun onAudioDevicesRemoved(
                removedDevices: Array<AudioDeviceInfo>) {
            Timber.d(" Audio devices removed")
            onAudioDeviceChange()
        }
    }

    /**
     * Helper method to trigger an audio route update when devices change. It
     * makes sure the operation is performed on the audio thread.
     */
    private fun onAudioDeviceChange() {
        callAudioManager.runInAudioThread(onAudioDeviceChangeRunner)
    }

    override fun start() {
        Timber.i("Using $this as the audio device handler")
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        onAudioDeviceChange()
    }

    override fun stop() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    companion object {
        /**
         * Constant defining a USB headset. Only available on API level >= 26.
         * The value of: AudioDeviceInfo.TYPE_USB_HEADSET
         */
        private const val TYPE_USB_HEADSET = 22
    }
}
