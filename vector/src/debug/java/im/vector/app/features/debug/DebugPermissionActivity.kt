/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.debug

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.onPermissionDeniedSnackbar
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.ActivityDebugPermissionBinding
import timber.log.Timber

class DebugPermissionActivity : VectorBaseActivity<ActivityDebugPermissionBinding>() {

    override fun getBinding() = ActivityDebugPermissionBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    // For debug
    private val allPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS)

    private var lastPermissions = emptyList<String>()

    override fun initUiAndData() {
        views.status.setOnClickListener { refresh() }

        views.camera.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.CAMERA)
            checkPerm()
        }
        views.audio.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.RECORD_AUDIO)
            checkPerm()
        }
        views.cameraAudio.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            checkPerm()
        }
        views.write.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            checkPerm()
        }
        views.read.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            checkPerm()
        }
        views.contact.setOnClickListener {
            lastPermissions = listOf(Manifest.permission.READ_CONTACTS)
            checkPerm()
        }
    }

    private fun checkPerm() {
        if (checkPermissions(lastPermissions, this, launcher, R.string.debug_rationale)) {
            Toast.makeText(this, "Already granted, sync call", Toast.LENGTH_SHORT).show()
        }
    }

    private var dialogOrSnackbar = false

    private val launcher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            Toast.makeText(this, "All granted", Toast.LENGTH_SHORT).show()
        } else {
            if (deniedPermanently) {
                dialogOrSnackbar = !dialogOrSnackbar
                if (dialogOrSnackbar) {
                    onPermissionDeniedDialog(R.string.denied_permission_generic)
                } else {
                    onPermissionDeniedSnackbar(R.string.denied_permission_generic)
                }
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        views.status.text = getStatus()
    }

    private fun getStatus(): String {
        return buildString {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Timber.v("## debugPermission() : log the permissions status used by the app")
                allPermissions.forEach { permission ->
                    append("[$permission] : ")
                    if (ContextCompat.checkSelfPermission(this@DebugPermissionActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                        append("PERMISSION_GRANTED")
                    } else {
                        append("PERMISSION_DENIED")
                    }
                    append(" show rational: ")
                    append(ActivityCompat.shouldShowRequestPermissionRationale(this@DebugPermissionActivity, permission))
                    append("\n")
                }
            } else {
                append("Before M!")
            }
            append("\n")
            append("(Click to refresh)")
        }
    }
}
