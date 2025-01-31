/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

/**
 * Parent class for all Android Services.
 */
abstract class VectorAndroidService : Service() {

    /**
     * Tells if the service self destroyed.
     */
    private var mIsSelfDestroyed = false

    override fun onCreate() {
        super.onCreate()

        Timber.i("## onCreate() : $this")
    }

    override fun onDestroy() {
        Timber.i("## onDestroy() : $this")

        if (!mIsSelfDestroyed) {
            Timber.w("## Destroy by the system : $this")
        }

        super.onDestroy()
    }

    protected fun myStopSelf() {
        mIsSelfDestroyed = true
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
