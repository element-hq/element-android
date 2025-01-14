/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.tracking

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSharingServiceConnection @Inject constructor(
        private val context: Context,
        private val activeSessionHolder: ActiveSessionHolder
) : ServiceConnection, LocationSharingAndroidService.Callback {

    interface Callback {
        fun onLocationServiceRunning(roomIds: Set<String>)
        fun onLocationServiceStopped()
        fun onLocationServiceError(error: Throwable)
    }

    private val callbacks = mutableSetOf<Callback>()
    private var isBound = false
    private var locationSharingAndroidService: LocationSharingAndroidService? = null

    fun bind(callback: Callback) {
        addCallback(callback)

        if (isBound) {
            callback.onLocationServiceRunning(getRoomIdsOfActiveLives())
        } else {
            Intent(context, LocationSharingAndroidService::class.java).also { intent ->
                context.bindService(intent, this, 0)
            }
        }
    }

    fun unbind(callback: Callback) {
        removeCallback(callback)
    }

    private fun getRoomIdsOfActiveLives(): Set<String> {
        return locationSharingAndroidService?.getRoomIdsOfActiveLives() ?: emptySet()
    }

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        locationSharingAndroidService = (binder as LocationSharingAndroidServiceBinder).getService()?.also { service ->
            service.callback = this
            getActiveSessionCoroutineScope()?.let { scope ->
                service.roomIdsOfActiveLives
                        .onEach(::onRoomIdsUpdate)
                        .launchIn(scope)
            }
        }
        isBound = true
    }

    private fun getActiveSessionCoroutineScope(): CoroutineScope? {
        return activeSessionHolder.getSafeActiveSession()?.coroutineScope
    }

    override fun onServiceDisconnected(className: ComponentName) {
        isBound = false
        locationSharingAndroidService?.callback = null
        locationSharingAndroidService = null
        onCallbackActionNoArg(Callback::onLocationServiceStopped)
    }

    private fun onRoomIdsUpdate(roomIds: Set<String>) {
        forwardRoomIdsToCallbacks(roomIds)
    }

    override fun onServiceError(error: Throwable) {
        forwardErrorToCallbacks(error)
    }

    private fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    private fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    private fun onCallbackActionNoArg(action: Callback.() -> Unit) {
        callbacks.toList().forEach(action)
    }

    private fun forwardRoomIdsToCallbacks(roomIds: Set<String>) {
        callbacks.toList().forEach { it.onLocationServiceRunning(roomIds) }
    }

    private fun forwardErrorToCallbacks(error: Throwable) {
        callbacks.toList().forEach { it.onLocationServiceError(error) }
    }
}
