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

package im.vector.app.features.location

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSharingServiceConnection @Inject constructor(
        private val context: Context
) : ServiceConnection, LocationSharingService.Callback {

    interface Callback {
        fun onLocationServiceRunning()
        fun onLocationServiceStopped()
        fun onLocationServiceError(error: Throwable)
    }

    private val callbacks = mutableSetOf<Callback>()
    private var isBound = false
    private var locationSharingService: LocationSharingService? = null

    fun bind(callback: Callback) {
        addCallback(callback)

        if (isBound) {
            callback.onLocationServiceRunning()
        } else {
            Intent(context, LocationSharingService::class.java).also { intent ->
                context.bindService(intent, this, 0)
            }
        }
    }

    fun unbind(callback: Callback) {
        removeCallback(callback)
    }

    fun stopLiveLocationSharing(roomId: String) {
        locationSharingService?.stopSharingLocation(roomId)
    }

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        locationSharingService = (binder as LocationSharingService.LocalBinder).getService().also {
            it.callback = this
        }
        isBound = true
        onCallbackActionNoArg(Callback::onLocationServiceRunning)
    }

    override fun onServiceDisconnected(className: ComponentName) {
        isBound = false
        locationSharingService?.callback = null
        locationSharingService = null
        onCallbackActionNoArg(Callback::onLocationServiceStopped)
    }

    override fun onServiceError(error: Throwable) {
        forwardErrorToCallbacks(error)
    }

    @Synchronized
    private fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    @Synchronized
    private fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    @Synchronized
    private fun onCallbackActionNoArg(action: Callback.() -> Unit) {
        callbacks.forEach(action)
    }

    @Synchronized
    private fun forwardErrorToCallbacks(error: Throwable) {
        callbacks.forEach { it.onLocationServiceError(error) }
    }
}
