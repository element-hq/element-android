/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * UnifiedPush lib tracks an action to check installed and uninstalled distributors.
 * We declare it to keep the background sync as an internal unifiedpush distributor.
 * This class is used to declare this action.
 */
class KeepInternalDistributor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
