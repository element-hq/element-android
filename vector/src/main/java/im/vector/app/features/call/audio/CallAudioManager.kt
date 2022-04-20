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

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import im.vector.app.R
import org.matrix.android.sdk.api.extensions.orFalse
import timber.log.Timber
import java.util.concurrent.Executors

class CallAudioManager(private val context: Context, val configChange: (() -> Unit)?) {

    private val audioManager: AudioManager? = context.getSystemService()
    private var audioDeviceDetector: AudioDeviceDetector? = null
    private var audioDeviceRouter: AudioDeviceRouter? = null

    sealed class Device(@StringRes val titleRes: Int, @DrawableRes val drawableRes: Int) {
        object Phone : Device(R.string.sound_device_phone, R.drawable.ic_sound_device_phone)
        object Speaker : Device(R.string.sound_device_speaker, R.drawable.ic_sound_device_speaker)
        object Headset : Device(R.string.sound_device_headset, R.drawable.ic_sound_device_headphone)
        data class WirelessHeadset(val name: String?) : Device(R.string.sound_device_wireless_headset, R.drawable.ic_sound_device_wireless)
    }

    enum class Mode {
        DEFAULT,
        AUDIO_CALL,
        VIDEO_CALL
    }

    private var mode = Mode.DEFAULT
    private var _availableDevices: MutableSet<Device> = HashSet()
    val availableDevices: Set<Device>
        get() = _availableDevices

    var selectedDevice: Device? = null
        private set
    private var userSelectedDevice: Device? = null

    init {
        runInAudioThread { setup() }
    }

    private fun setup() {
        if (audioManager == null) {
            return
        }
        audioDeviceDetector?.stop()
        audioDeviceDetector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            API23AudioDeviceDetector(audioManager, this)
        } else {
            API21AudioDeviceDetector(context, audioManager, this)
        }
        audioDeviceDetector?.start()
        audioDeviceRouter = DefaultAudioDeviceRouter(audioManager, this)
    }

    fun runInAudioThread(runnable: Runnable) {
        executor.execute(runnable)
    }

    /**
     * Sets the user selected audio device as the active audio device.
     *
     * @param device the desired device which will become active.
     */
    fun setAudioDevice(device: Device) {
        runInAudioThread(Runnable {
            if (!_availableDevices.contains(device)) {
                Timber.w("Audio device not available: $device")
                userSelectedDevice = null
                return@Runnable
            }
            if (mode != Mode.DEFAULT) {
                Timber.i("User selected device set to: $device")
                userSelectedDevice = device
                updateAudioRoute(mode, false)
            }
        })
    }

    /**
     * Public method to set the current audio mode.
     *
     * @param mode the desired audio mode.
     * could be updated successfully, and it will be rejected otherwise.
     */
    fun setMode(mode: Mode) {
        runInAudioThread {
            var success: Boolean
            try {
                success = updateAudioRoute(mode, false)
            } catch (e: Throwable) {
                success = false
                Timber.e(e, "Failed to update audio route for mode: $mode")
            }
            if (success) {
                this@CallAudioManager.mode = mode
            }
        }
    }

    /**
     * Updates the audio route for the given mode.
     *
     * @param mode the audio mode to be used when computing the audio route.
     * @return `true` if the audio route was updated successfully;
     * `false`, otherwise.
     */
    private fun updateAudioRoute(mode: Mode, force: Boolean): Boolean {
        Timber.i("Update audio route for mode: $mode")
        if (!audioDeviceRouter?.setMode(mode).orFalse()) {
            return false
        }
        if (mode == Mode.DEFAULT) {
            selectedDevice = null
            userSelectedDevice = null
            return true
        }
        val availableBluetoothDevice = _availableDevices.firstOrNull { it is Device.WirelessHeadset }
        val headsetAvailable = _availableDevices.contains(Device.Headset)

        // Pick the desired device based on what's available and the mode.
        var audioDevice: Device
        audioDevice = if (availableBluetoothDevice != null) {
            availableBluetoothDevice
        } else if (headsetAvailable) {
            Device.Headset
        } else if (mode == Mode.VIDEO_CALL) {
            Device.Speaker
        } else {
            Device.Phone
        }
        // Consider the user's selection
        if (userSelectedDevice != null && _availableDevices.contains(userSelectedDevice)) {
            audioDevice = userSelectedDevice!!
        }

        // If the previously selected device and the current default one
        // match, do nothing.
        if (!force && selectedDevice != null && selectedDevice == audioDevice) {
            return true
        }
        selectedDevice = audioDevice
        Timber.i("Selected audio device: $audioDevice")
        audioDeviceRouter?.setAudioRoute(audioDevice)
        configChange?.invoke()
        return true
    }

    /**
     * Resets the current device selection.
     */
    fun resetSelectedDevice() {
        selectedDevice = null
        userSelectedDevice = null
    }

    /**
     * Adds a new device to the list of available devices.
     *
     * @param device The new device.
     */
    fun addDevice(device: Device) {
        _availableDevices.add(device)
        resetSelectedDevice()
    }

    /**
     * Removes a device from the list of available devices.
     *
     * @param device The old device to the removed.
     */
    fun removeDevice(device: Device) {
        _availableDevices.remove(device)
        resetSelectedDevice()
    }

    /**
     * Replaces the current list of available devices with a new one.
     *
     * @param devices The new devices list.
     */
    fun replaceDevices(devices: Set<Device>) {
        _availableDevices.clear()
        _availableDevices.addAll(devices)
        resetSelectedDevice()
    }

    /**
     * Re-sets the current audio route. Needed when devices changes have happened.
     */
    fun updateAudioRoute() {
        if (mode != Mode.DEFAULT) {
            updateAudioRoute(mode, false)
        }
    }

    /**
     * Re-sets the current audio route. Needed when focus is lost and regained.
     */
    fun resetAudioRoute() {
        if (mode != Mode.DEFAULT) {
            updateAudioRoute(mode, true)
        }
    }

    /**
     * Interface for the modules implementing the actual audio device management.
     */
    interface AudioDeviceDetector {
        /**
         * Start detecting audio device changes.
         */
        fun start()

        /**
         * Stop audio device detection.
         */
        fun stop()
    }

    interface AudioDeviceRouter {
        /**
         * Set the appropriate route for the given audio device.
         *
         * @param device Audio device for which the route must be set.
         */
        fun setAudioRoute(device: Device)

        /**
         * Set the given audio mode.
         *
         * @param mode The new audio mode to be used.
         * @return Whether the operation was successful or not.
         */
        fun setMode(mode: Mode): Boolean
    }

    companion object {
        // Every audio operations should be launched on single thread
        private val executor = Executors.newSingleThreadExecutor()
    }
}
