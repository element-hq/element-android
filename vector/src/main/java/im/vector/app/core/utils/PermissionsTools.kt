/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import timber.log.Timber

// Permissions sets
val PERMISSIONS_FOR_AUDIO_IP_CALL = listOf(Manifest.permission.RECORD_AUDIO)
val PERMISSIONS_FOR_VIDEO_IP_CALL = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
val PERMISSIONS_FOR_TAKING_PHOTO = listOf(Manifest.permission.CAMERA)
val PERMISSIONS_FOR_MEMBERS_SEARCH = listOf(Manifest.permission.READ_CONTACTS)
val PERMISSIONS_FOR_ROOM_AVATAR = listOf(Manifest.permission.CAMERA)
val PERMISSIONS_FOR_WRITING_FILES = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
val PERMISSIONS_FOR_PICKING_CONTACT = listOf(Manifest.permission.READ_CONTACTS)

val PERMISSIONS_EMPTY = emptyList<String>()

// For debug
val PERMISSIONS_ALL = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS)

fun ComponentActivity.registerForPermissionsResult(allGranted: (Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        allGranted.invoke(result.keys.all { result[it] == true })
    }
}

fun Fragment.registerForPermissionsResult(allGranted: (Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        allGranted.invoke(result.keys.all { result[it] == true })
    }
}

/**
 * Check if the permissions provided in the list are granted.
 * This is an asynchronous method if permissions are requested, the final response
 * is provided in onRequestPermissionsResult(). In this case checkPermissions()
 * returns false.
 * <br></br>If checkPermissions() returns true, the permissions were already granted.
 * The permissions to be granted are given as bit map in permissionsToBeGrantedBitMap (ex: [.PERMISSIONS_FOR_TAKING_PHOTO]).
 * <br></br>permissionsToBeGrantedBitMap is passed as the request code in onRequestPermissionsResult().
 *
 *
 * If a permission was already denied by the user, a popup is displayed to
 * explain why vector needs the corresponding permission.
 *
 * @param permissionsToBeGranted the permissions to be granted
 * @param activity               the calling Activity that is requesting the permissions (or fragment parent)
 * @param activityResultLauncher from the calling fragment/Activity that is requesting the permissions
 * @return true if the permissions are granted (synchronous flow), false otherwise (asynchronous flow)
 */
fun checkPermissions(permissionsToBeGranted: List<String>,
                     activity: Activity,
                     activityResultLauncher: ActivityResultLauncher<Array<String>>,
                     @StringRes rationaleMessage: Int = 0): Boolean {
    var isPermissionGranted = false

    // sanity check
    if (permissionsToBeGranted.isEmpty()) {
        isPermissionGranted = true
    } else {
        val permissionListAlreadyDenied = mutableListOf<String>()
        val permissionsListToBeGranted = mutableListOf<String>()
        var isRequestPermissionRequired = false

        // retrieve the permissions to be granted according to the permission list
        permissionsToBeGranted.forEach { permission ->
            if (updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permission)) {
                isRequestPermissionRequired = true
            }
        }

        // if some permissions were already denied: display a dialog to the user before asking again.
        if (permissionListAlreadyDenied.isNotEmpty() && rationaleMessage != 0) {
            // display the dialog with the info text
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.permissions_rationale_popup_title)
                    .setMessage(rationaleMessage)
                    .setOnCancelListener { Toast.makeText(activity, R.string.missing_permissions_warning, Toast.LENGTH_SHORT).show() }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (permissionsListToBeGranted.isNotEmpty()) {
                            activityResultLauncher.launch(permissionsListToBeGranted.toTypedArray())
                        }
                    }
                    .show()
        } else {
            // some permissions are not granted, ask permissions
            if (isRequestPermissionRequired) {
                activityResultLauncher.launch(permissionsListToBeGranted.toTypedArray())
            } else {
                // permissions were granted, start now.
                isPermissionGranted = true
            }
        }
    }

    return isPermissionGranted
}

fun VectorBaseActivity<*>.onPermissionDeniedSnackbar(@StringRes rationaleMessage: Int) {
    showSnackbar(getString(rationaleMessage), R.string.settings) {
        openAppSettingsPage(this)
    }
}

/**
 * Helper method used in [.checkPermissions] to populate the list of the
 * permissions to be granted (permissionsListToBeGrantedOut) and the list of the permissions already denied (permissionAlreadyDeniedListOut).
 *
 * @param activity                       calling activity
 * @param permissionAlreadyDeniedListOut list to be updated with the permissions already denied by the user
 * @param permissionsListToBeGrantedOut  list to be updated with the permissions to be granted
 * @param permissionType                 the permission to be checked
 * @return true if the permission requires to be granted, false otherwise
 */
private fun updatePermissionsToBeGranted(activity: Activity,
                                         permissionAlreadyDeniedListOut: MutableList<String>,
                                         permissionsListToBeGrantedOut: MutableList<String>,
                                         permissionType: String): Boolean {
    var isRequestPermissionRequested = false

    // add permission to be granted
    permissionsListToBeGrantedOut.add(permissionType)

    if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity.applicationContext, permissionType)) {
        isRequestPermissionRequested = true

        // add permission to the ones that were already asked to the user
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionType)) {
            permissionAlreadyDeniedListOut.add(permissionType)
        }
    }
    return isRequestPermissionRequested
}
