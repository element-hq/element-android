/*
 * Copyright 2019 New Vector Ltd
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
