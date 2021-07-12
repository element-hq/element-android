/*
 * Copyright 2019 New Vector Ltd
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

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.PERMISSIONS_ALL
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.ActivityDebugPermissionBinding
import timber.log.Timber

class DebugPermissionActivity : VectorBaseActivity<ActivityDebugPermissionBinding>() {

    override fun getBinding() = ActivityDebugPermissionBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        views.status.setOnClickListener { refresh() }

        listOf(
                views.audio,
                views.camera,
                views.write,
                views.read,
                views.contact
        ).forEach { button ->
            button.setOnClickListener {
                checkPermissions(listOf(button.text.toString()), this, launcher, R.string.debug_rationale)
            }
        }
    }

    private val launcher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            Toast.makeText(this, "All granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
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
                PERMISSIONS_ALL.forEach { permission ->
                    append("[$permission] : ")
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this@DebugPermissionActivity, permission)) {
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
