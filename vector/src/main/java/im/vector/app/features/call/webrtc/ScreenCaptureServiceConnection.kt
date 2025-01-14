/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.webrtc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import javax.inject.Inject

class ScreenCaptureServiceConnection @Inject constructor(
        private val context: Context
) : ServiceConnection {

    interface Callback {
        fun onServiceConnected()
    }

    private var isBound = false
    private var screenCaptureAndroidService: ScreenCaptureAndroidService? = null
    private var callback: Callback? = null

    fun bind(callback: Callback) {
        this.callback = callback

        if (isBound) {
            callback.onServiceConnected()
        } else {
            Intent(context, ScreenCaptureAndroidService::class.java).also { intent ->
                context.bindService(intent, this, 0)
            }
        }
    }

    fun unbind() {
        callback = null
    }

    fun stopScreenCapturing() {
        screenCaptureAndroidService?.stopService()
    }

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        screenCaptureAndroidService = (binder as ScreenCaptureAndroidService.LocalBinder).getService()
        isBound = true
        callback?.onServiceConnected()
    }

    override fun onServiceDisconnected(className: ComponentName) {
        isBound = false
        screenCaptureAndroidService = null
        callback = null
    }
}
