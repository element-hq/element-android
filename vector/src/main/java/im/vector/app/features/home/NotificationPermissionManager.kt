/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import im.vector.app.core.utils.checkPermissions
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import javax.inject.Inject

class NotificationPermissionManager @Inject constructor(
        private val sdkIntProvider: BuildVersionSdkIntProvider,
        private val vectorPreferences: VectorPreferences,
) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionGranted(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun eventuallyRequestPermission(
            activity: Activity,
            requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
            showRationale: Boolean = true,
            ignorePreference: Boolean = false,
    ) {
        if (!sdkIntProvider.isAtLeast(Build.VERSION_CODES.TIRAMISU)) return
        if (!vectorPreferences.areNotificationEnabledForDevice() && !ignorePreference) return
        checkPermissions(
                listOf(Manifest.permission.POST_NOTIFICATIONS),
                activity,
                activityResultLauncher = requestPermissionLauncher,
                if (showRationale) CommonStrings.permissions_rationale_msg_notification else 0
        )
    }

    @RequiresApi(33)
    fun askPermission(requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    fun eventuallyRevokePermission(
            activity: Activity,
    ) {
        if (!sdkIntProvider.isAtLeast(Build.VERSION_CODES.TIRAMISU)) return
        activity.revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)
    }
}
