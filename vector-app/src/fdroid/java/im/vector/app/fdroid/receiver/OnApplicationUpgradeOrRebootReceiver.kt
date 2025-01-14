/*
 * Copyright 2019-2024 New Vector Ltd.
 * Copyright 2018 New Vector Ltd
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.fdroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.fdroid.BackgroundSyncStarter
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class OnApplicationUpgradeOrRebootReceiver : BroadcastReceiver() {

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var backgroundSyncStarter: BackgroundSyncStarter

    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("## onReceive() ${intent.action}")
        backgroundSyncStarter.start(activeSessionHolder)
    }
}
