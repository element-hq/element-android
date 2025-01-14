/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
@file:Suppress("DEPRECATION")

package im.vector.app.features.call.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import im.vector.app.core.services.BluetoothHeadsetReceiver
import im.vector.app.core.services.WiredHeadsetStateReceiver
import org.matrix.android.sdk.api.logger.LoggerTag
import timber.log.Timber

private val loggerTag = LoggerTag("API21AudioDeviceDetector", LoggerTag.VOIP)

internal class API21AudioDeviceDetector(
        private val context: Context,
        private val audioManager: AudioManager,
        private val callAudioManager: CallAudioManager
) : CallAudioManager.AudioDeviceDetector, WiredHeadsetStateReceiver.HeadsetEventListener, BluetoothHeadsetReceiver.EventListener {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectedBlueToothHeadset: BluetoothProfile? = null
    private var wiredHeadsetStateReceiver: WiredHeadsetStateReceiver? = null
    private var bluetoothHeadsetStateReceiver: BluetoothHeadsetReceiver? = null

    private val onAudioDeviceChangeRunner = Runnable {
        val devices = getAvailableSoundDevices()
        callAudioManager.replaceDevices(devices)
        Timber.i(" Available audio devices: $devices")
        callAudioManager.updateAudioRoute()
    }

    private fun getAvailableSoundDevices(): Set<CallAudioManager.Device> {
        return HashSet<CallAudioManager.Device>().apply {
            if (isBluetoothHeadsetOn()) {
                connectedBlueToothHeadset?.connectedDevices?.forEach {
                    add(CallAudioManager.Device.WirelessHeadset(it.name))
                }
            }
            if (isWiredHeadsetOn()) {
                add(CallAudioManager.Device.Headset)
            } else {
                add(CallAudioManager.Device.Phone)
            }
            add(CallAudioManager.Device.Speaker)
        }
    }

    private fun isWiredHeadsetOn(): Boolean {
        return audioManager.isWiredHeadsetOn
    }

    private fun isBluetoothHeadsetOn(): Boolean {
        Timber.tag(loggerTag.value).v("AudioManager isBluetoothHeadsetOn")
        try {
            if (connectedBlueToothHeadset == null) return false.also {
                Timber.tag(loggerTag.value).v("AudioManager no connected bluetooth headset")
            }
            if (!audioManager.isBluetoothScoAvailableOffCall) return false.also {
                Timber.tag(loggerTag.value).v("AudioManager isBluetoothScoAvailableOffCall false")
            }
            return true
        } catch (failure: Throwable) {
            Timber.e("AudioManager isBluetoothHeadsetOn failure ${failure.localizedMessage}")
            return false
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
        Timber.i("Start using $this as the audio device handler")
        wiredHeadsetStateReceiver = WiredHeadsetStateReceiver.createAndRegister(context, this)
        bluetoothHeadsetStateReceiver = BluetoothHeadsetReceiver.createAndRegister(context, this)
        val bm: BluetoothManager? = context.getSystemService()
        val adapter = bm?.adapter
        Timber.tag(loggerTag.value).d("Bluetooth adapter $adapter")
        bluetoothAdapter = adapter
        adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceDisconnected(profile: Int) {
                Timber.tag(loggerTag.value).d("onServiceDisconnected $profile")
                if (profile == BluetoothProfile.HEADSET) {
                    connectedBlueToothHeadset = null
                    onAudioDeviceChange()
                }
            }

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                Timber.tag(loggerTag.value).d("onServiceConnected $profile , proxy:$proxy")
                if (profile == BluetoothProfile.HEADSET) {
                    connectedBlueToothHeadset = proxy
                    onAudioDeviceChange()
                }
            }
        }, BluetoothProfile.HEADSET)
        onAudioDeviceChange()
    }

    override fun stop() {
        Timber.i("Stop using $this as the audio device handler")
        wiredHeadsetStateReceiver?.let { WiredHeadsetStateReceiver.unRegister(context, it) }
        wiredHeadsetStateReceiver = null
        bluetoothHeadsetStateReceiver?.let { BluetoothHeadsetReceiver.unRegister(context, it) }
        bluetoothHeadsetStateReceiver = null
    }

    override fun onHeadsetEvent(event: WiredHeadsetStateReceiver.HeadsetPlugEvent) {
        Timber.tag(loggerTag.value).v("onHeadsetEvent $event")
        onAudioDeviceChange()
    }

    override fun onBTHeadsetEvent(event: BluetoothHeadsetReceiver.BTHeadsetPlugEvent) {
        Timber.tag(loggerTag.value).v("onBTHeadsetEvent $event")
        onAudioDeviceChange()
    }
}
