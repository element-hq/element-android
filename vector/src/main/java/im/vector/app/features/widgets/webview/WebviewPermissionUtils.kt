/*
 * Copyright (c) 2020 New Vector Ltd
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
package im.vector.app.features.widgets.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.PermissionRequest
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R

object WebviewPermissionUtils {

    @SuppressLint("NewApi")
    fun promptForPermissions(@StringRes title: Int, request: PermissionRequest, context: Context) {
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
                .setPositiveButton(R.string.room_widget_resource_grant_permission) { _, _ ->
                    request.grant(allowedPermissions.mapNotNull { perm ->
                        perm.first.takeIf { perm.second }
                    }.toTypedArray())
                }
                .setNegativeButton(R.string.room_widget_resource_decline_permission) { _, _ ->
                    request.deny()
                }
                .show()
    }

    private fun webPermissionToHumanReadable(permission: String, context: Context): String {
        return when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE      -> context.getString(R.string.room_widget_webview_access_microphone)
            PermissionRequest.RESOURCE_VIDEO_CAPTURE      -> context.getString(R.string.room_widget_webview_access_camera)
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> context.getString(R.string.room_widget_webview_read_protected_media)
            else                                          -> permission
        }
    }
}
