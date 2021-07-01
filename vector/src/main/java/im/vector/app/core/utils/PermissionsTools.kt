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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
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

// Android M permission request code management
private const val PERMISSIONS_GRANTED = true
private const val PERMISSIONS_DENIED = !PERMISSIONS_GRANTED

// Permission bit
private const val PERMISSION_BYPASSED = 0x0
const val PERMISSION_CAMERA = 0x1
private const val PERMISSION_WRITE_EXTERNAL_STORAGE = 0x1 shl 1
private const val PERMISSION_RECORD_AUDIO = 0x1 shl 2
private const val PERMISSION_READ_CONTACTS = 0x1 shl 3
private const val PERMISSION_READ_EXTERNAL_STORAGE = 0x1 shl 4

// Permissions sets
const val PERMISSIONS_FOR_AUDIO_IP_CALL = PERMISSION_RECORD_AUDIO
const val PERMISSIONS_FOR_VIDEO_IP_CALL = PERMISSION_CAMERA or PERMISSION_RECORD_AUDIO
const val PERMISSIONS_FOR_TAKING_PHOTO = PERMISSION_CAMERA
const val PERMISSIONS_FOR_MEMBERS_SEARCH = PERMISSION_READ_CONTACTS
const val PERMISSIONS_FOR_MEMBER_DETAILS = PERMISSION_READ_CONTACTS
const val PERMISSIONS_FOR_ROOM_AVATAR = PERMISSION_CAMERA
const val PERMISSIONS_FOR_VIDEO_RECORDING = PERMISSION_CAMERA or PERMISSION_RECORD_AUDIO
const val PERMISSIONS_FOR_WRITING_FILES = PERMISSION_WRITE_EXTERNAL_STORAGE
const val PERMISSIONS_FOR_READING_FILES = PERMISSION_READ_EXTERNAL_STORAGE
const val PERMISSIONS_FOR_PICKING_CONTACT = PERMISSION_READ_CONTACTS

const val PERMISSIONS_EMPTY = PERMISSION_BYPASSED

// Request code to ask permission to the system (arbitrary values)
const val PERMISSION_REQUEST_CODE = 567
const val PERMISSION_REQUEST_CODE_LAUNCH_CAMERA = 568
const val PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_CAMERA = 569
const val PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_VIDEO_CAMERA = 570
const val PERMISSION_REQUEST_CODE_AUDIO_CALL = 571
const val PERMISSION_REQUEST_CODE_VIDEO_CALL = 572
const val PERMISSION_REQUEST_CODE_CHANGE_AVATAR = 574
const val PERMISSION_REQUEST_CODE_DOWNLOAD_FILE = 575
const val PERMISSION_REQUEST_CODE_PICK_ATTACHMENT = 576
const val PERMISSION_REQUEST_CODE_INCOMING_URI = 577
const val PERMISSION_REQUEST_CODE_READ_CONTACTS = 579

/**
 * Log the used permissions statuses.
 */
fun logPermissionStatuses(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS)

        Timber.v("## logPermissionStatuses() : log the permissions status used by the app")

        for (permission in permissions) {
            Timber.v(("Status of [$permission] : " +
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission)) {
                        "PERMISSION_GRANTED"
                    } else {
                        "PERMISSION_DENIED"
                    }))
        }
    }
}

fun Fragment.registerForPermissionsResult(allGranted: (Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        allGranted.invoke(result.keys.all { result[it] == true })
    }
}

/**
 * See [.checkPermissions]
 *
 * @param permissionsToBeGrantedBitMap
 * @param activity
 * @return true if the permissions are granted (synchronous flow), false otherwise (asynchronous flow)
 */
fun checkPermissions(permissionsToBeGrantedBitMap: Int,
                     activity: Activity,
                     requestCode: Int,
                     @StringRes rationaleMessage: Int = 0): Boolean {
    return checkPermissions(permissionsToBeGrantedBitMap, activity, null, requestCode, rationaleMessage)
}

/**
 * See [.checkPermissions]
 *
 * @param permissionsToBeGrantedBitMap
 * @param activityResultLauncher       from the calling fragment that is requesting the permissions
 * @return true if the permissions are granted (synchronous flow), false otherwise (asynchronous flow)
 */
fun checkPermissions(permissionsToBeGrantedBitMap: Int,
                     activity: Activity,
                     activityResultLauncher: ActivityResultLauncher<Array<String>>,
                     @StringRes rationaleMessage: Int = 0): Boolean {
    return checkPermissions(permissionsToBeGrantedBitMap, activity, activityResultLauncher, 0, rationaleMessage)
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
 * @param permissionsToBeGrantedBitMap the permissions bit map to be granted
 * @param activity                     the calling Activity that is requesting the permissions (or fragment parent)
 * @param activityResultLauncher       from the calling fragment that is requesting the permissions
 * @return true if the permissions are granted (synchronous flow), false otherwise (asynchronous flow)
 */
private fun checkPermissions(permissionsToBeGrantedBitMap: Int,
                             activity: Activity,
                             activityResultLauncher: ActivityResultLauncher<Array<String>>?,
                             requestCode: Int,
                             @StringRes rationaleMessage: Int
): Boolean {
    var isPermissionGranted = false

    // sanity check
    if (PERMISSIONS_EMPTY == permissionsToBeGrantedBitMap) {
        isPermissionGranted = true
    } else if (PERMISSIONS_FOR_AUDIO_IP_CALL != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_VIDEO_IP_CALL != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_TAKING_PHOTO != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_MEMBERS_SEARCH != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_MEMBER_DETAILS != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_ROOM_AVATAR != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_VIDEO_RECORDING != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_WRITING_FILES != permissionsToBeGrantedBitMap
            && PERMISSIONS_FOR_READING_FILES != permissionsToBeGrantedBitMap) {
        Timber.w("## checkPermissions(): permissions to be granted are not supported")
        isPermissionGranted = false
    } else {
        val permissionListAlreadyDenied = ArrayList<String>()
        val permissionsListToBeGranted = ArrayList<String>()
        var isRequestPermissionRequired = false

        // retrieve the permissions to be granted according to the request code bit map
        if (PERMISSION_CAMERA == permissionsToBeGrantedBitMap and PERMISSION_CAMERA) {
            val permissionType = Manifest.permission.CAMERA
            isRequestPermissionRequired = isRequestPermissionRequired or
                    updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permissionType)
        }

        if (PERMISSION_RECORD_AUDIO == permissionsToBeGrantedBitMap and PERMISSION_RECORD_AUDIO) {
            val permissionType = Manifest.permission.RECORD_AUDIO
            isRequestPermissionRequired = isRequestPermissionRequired or
                    updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permissionType)
        }

        if (PERMISSION_WRITE_EXTERNAL_STORAGE == permissionsToBeGrantedBitMap and PERMISSION_WRITE_EXTERNAL_STORAGE) {
            val permissionType = Manifest.permission.WRITE_EXTERNAL_STORAGE
            isRequestPermissionRequired = isRequestPermissionRequired or
                    updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permissionType)
        }

        if (PERMISSION_READ_EXTERNAL_STORAGE == permissionsToBeGrantedBitMap and PERMISSION_READ_EXTERNAL_STORAGE) {
            val permissionType = Manifest.permission.READ_EXTERNAL_STORAGE
            isRequestPermissionRequired = isRequestPermissionRequired or
                    updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permissionType)
        }

        // the contact book access is requested for any android platforms
        // for android M, we use the system preferences
        // for android < M, we use a dedicated settings
        if (PERMISSION_READ_CONTACTS == permissionsToBeGrantedBitMap and PERMISSION_READ_CONTACTS) {
            val permissionType = Manifest.permission.READ_CONTACTS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isRequestPermissionRequired = isRequestPermissionRequired or
                        updatePermissionsToBeGranted(activity, permissionListAlreadyDenied, permissionsListToBeGranted, permissionType)
            } else {
                // TODO uncomment
                /*if (!ContactsManager.getInstance().isContactBookAccessRequested) {
                    isRequestPermissionRequired = true
                    permissionsListToBeGranted.add(permissionType)
                }*/
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
                            activityResultLauncher
                                    ?.launch(permissionsListToBeGranted.toTypedArray())
                                    ?: run {
                                        ActivityCompat.requestPermissions(activity, permissionsListToBeGranted.toTypedArray(), requestCode)
                                    }
                        }
                    }
                    .show()
        } else {
            // some permissions are not granted, ask permissions
            if (isRequestPermissionRequired) {
                val permissionsArrayToBeGranted = permissionsListToBeGranted.toTypedArray()

                // for android < M, we use a custom dialog to request the contacts book access.
                if (permissionsListToBeGranted.contains(Manifest.permission.READ_CONTACTS)
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    TODO()
                    /*
                    MaterialAlertDialogBuilder(activity)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle(R.string.permissions_rationale_popup_title)
                            .setMessage(R.string.permissions_msg_contacts_warning_other_androids)
                            // gives the contacts book access
                            .setPositiveButton(R.string.yes) { _, _ ->
                                ContactsManager.getInstance().setIsContactBookAccessAllowed(true)
                                fragment?.requestPermissions(permissionsArrayToBeGranted, requestCode)
                                        ?: run {
                                            ActivityCompat.requestPermissions(activity, permissionsArrayToBeGranted, requestCode)
                                        }
                            }
                            // or reject it
                            .setNegativeButton(R.string.no) { _, _ ->
                                ContactsManager.getInstance().setIsContactBookAccessAllowed(false)
                                fragment?.requestPermissions(permissionsArrayToBeGranted, requestCode)
                                        ?: run {
                                            ActivityCompat.requestPermissions(activity, permissionsArrayToBeGranted, requestCode)
                                        }
                            }
                            .show()
                    */
                } else {
                    activityResultLauncher
                            ?.launch(permissionsArrayToBeGranted)
                            ?: run {
                                ActivityCompat.requestPermissions(activity, permissionsArrayToBeGranted, requestCode)
                            }
                }
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

/**
 * Return true if all permissions are granted, false if not or if permission request has been cancelled
 */
fun allGranted(grantResults: IntArray): Boolean {
    if (grantResults.isEmpty()) {
        // A cancellation occurred
        return false
    }

    var granted = true

    grantResults.forEach {
        granted = granted && PackageManager.PERMISSION_GRANTED == it
    }

    return granted
}
