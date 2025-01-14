/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.fdroid.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.settings.VectorPreferences
import timber.log.Timber
import javax.inject.Inject

class FDroidGuardServiceStarter @Inject constructor(
        private val preferences: VectorPreferences,
        private val appContext: Context
) : GuardServiceStarter {

    override fun start() {
        if (preferences.isBackgroundSyncEnabled()) {
            try {
                Timber.i("## Sync: starting GuardService")
                val intent = Intent(appContext, GuardAndroidService::class.java)
                ContextCompat.startForegroundService(appContext, intent)
            } catch (ex: Throwable) {
                Timber.e("## Sync: ERROR starting GuardService")
            }
        }
    }

    override fun stop() {
        val intent = Intent(appContext, GuardAndroidService::class.java)
        appContext.stopService(intent)
    }
}
