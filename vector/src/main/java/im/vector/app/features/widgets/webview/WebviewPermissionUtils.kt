/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.widgets.webview

import android.Manifest
import android.content.Context
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.error.fatalError
import im.vector.app.core.utils.checkPermissions
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class WebviewPermissionUtils @Inject constructor(
        private val vectorPreferences: VectorPreferences,
) {

    private var permissionRequest: PermissionRequest? = null
    private var selectedPermissions = listOf<String>()

    fun promptForPermissions(
            @StringRes title: Int,
            request: PermissionRequest,
            context: Context,
            activity: FragmentActivity,
            activityResultLauncher: ActivityResultLauncher<Array<String>>,
            autoApprove: Boolean = false
    ) {
        if (autoApprove) {
            onPermissionsSelected(
                permissions = request.resources.toList(),
                request = request,
                activity = activity,
                activityResultLauncher = activityResultLauncher)
            return
        }

        val allowedPermissions = request.resources.map {
            it to false
        }.toMutableList()

        MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMultiChoiceItems(
                        request.resources.map { webPermissionToHumanReadable(it, context) }.toTypedArray(), null
                ) { _, which, isChecked ->
                    allowedPermissions[which] = allowedPermissions[which].first to isChecked
                }
                .setPositiveButton(CommonStrings.room_widget_resource_grant_permission) { _, _ ->
                    val permissions = allowedPermissions.mapNotNull { perm ->
                        perm.first.takeIf { perm.second }
                    }
                    onPermissionsSelected(permissions, request, activity, activityResultLauncher)
                }
                .setNegativeButton(CommonStrings.room_widget_resource_decline_permission) { _, _ ->
                    request.deny()
                }
                .show()
    }

    private fun onPermissionsSelected(
            permissions: List<String>,
            request: PermissionRequest,
            activity: FragmentActivity,
            activityResultLauncher: ActivityResultLauncher<Array<String>>,
    ) {
        permissionRequest = request
        selectedPermissions = permissions

        val requiredAndroidPermissions = selectedPermissions.mapNotNull { permission ->
            webPermissionToAndroidPermission(permission)
        }

        // When checkPermissions returns false, some of the required Android permissions will
        // have to be requested and the flow completes asynchronously via onPermissionResult
        if (checkPermissions(requiredAndroidPermissions, activity, activityResultLauncher)) {
            request.grant(selectedPermissions.toTypedArray())
            reset()
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>) {
        if (permissionRequest == null) {
            fatalError(
                    message = "permissionRequest was null! Make sure to call promptForPermissions first.",
                    failFast = vectorPreferences.failFast()
            )
            return
        }
        val grantedPermissions = filterPermissionsToBeGranted(selectedPermissions, result)
        if (grantedPermissions.isNotEmpty()) {
            permissionRequest?.grant(grantedPermissions.toTypedArray())
        } else {
            permissionRequest?.deny()
        }
        reset()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun filterPermissionsToBeGranted(selectedWebPermissions: List<String>, androidPermissionResult: Map<String, Boolean>): List<String> {
        return selectedWebPermissions.filter { webPermission ->
            val androidPermission = webPermissionToAndroidPermission(webPermission)
                    ?: return@filter true // No corresponding Android permission exists
            return@filter androidPermissionResult[androidPermission]
                    ?: return@filter true // Android permission already granted before
        }
    }

    private fun reset() {
        permissionRequest = null
        selectedPermissions = listOf()
    }

    private fun webPermissionToHumanReadable(permission: String, context: Context): String {
        return when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> context.getString(CommonStrings.room_widget_webview_access_microphone)
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> context.getString(CommonStrings.room_widget_webview_access_camera)
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> context.getString(CommonStrings.room_widget_webview_read_protected_media)
            else -> permission
        }
    }

    private fun webPermissionToAndroidPermission(permission: String): String? {
        return when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
            else -> null
        }
    }
}
