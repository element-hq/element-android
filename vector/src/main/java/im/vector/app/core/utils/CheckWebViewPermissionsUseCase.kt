/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat
import javax.inject.Inject

class CheckWebViewPermissionsUseCase @Inject constructor() {

    /**
     * Checks if required WebView permissions are already granted system level.
     * @param activity the calling Activity that is requesting the permissions (or fragment parent)
     * @param request WebView permission request of onPermissionRequest function
     * @return true if WebView permissions are already granted, false otherwise
     */
    fun execute(activity: Activity, request: PermissionRequest): Boolean {
        return request.resources.all {
            when (it) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    PERMISSIONS_FOR_AUDIO_IP_CALL.all { permission ->
                        ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    PERMISSIONS_FOR_VIDEO_IP_CALL.all { permission ->
                        ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
                else -> {
                    false
                }
            }
        }
    }
}
