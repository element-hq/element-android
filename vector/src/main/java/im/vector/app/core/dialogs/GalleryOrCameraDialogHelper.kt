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

package im.vector.app.core.dialogs

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.entity.MultiPickerImageType

class GalleryOrCameraDialogHelper(
        private val fragment: Fragment
) {
    interface Listener {
        fun onImageReady(image: MultiPickerImageType)
    }

    private val activity by lazy { fragment.requireActivity() }

    private val listener: Listener = fragment as? Listener ?: error("Fragment must implements GalleryOrCameraDialogHelper.Listener")

    private val takePhotoPermissionActivityResultLauncher = fragment.registerForPermissionsResult { allGranted ->
        if (allGranted) {
            doOpenCamera()
        }
    }

    private val takePhotoActivityResultLauncher = fragment.registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            avatarCameraUri?.let { uri ->
                MultiPicker.get(MultiPicker.CAMERA)
                        .getTakenPhoto(fragment.requireContext(), uri)
                        ?.let { listener.onImageReady(it) }
            }
        }
    }

    private val pickImageActivityResultLauncher = fragment.registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            MultiPicker
                    .get(MultiPicker.IMAGE)
                    .getSelectedFiles(fragment.requireContext(), activityResult.data)
                    .firstOrNull()
                    ?.let { listener.onImageReady(it) }
        }
    }

    private enum class Type {
        Gallery,
        Camera
    }

    fun show() {
        AlertDialog.Builder(fragment.requireContext())
                .setItems(arrayOf(
                        fragment.getString(R.string.attachment_type_camera),
                        fragment.getString(R.string.attachment_type_gallery)
                )) { dialog, which ->
                    dialog.cancel()
                    onAvatarTypeSelected(if (which == 0) Type.Camera else Type.Gallery)
                }
                .show()
    }

    private fun onAvatarTypeSelected(type: Type) {
        when (type) {
            Type.Gallery ->
                MultiPicker.get(MultiPicker.IMAGE).single().startWith(pickImageActivityResultLauncher)
            Type.Camera ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, activity, takePhotoPermissionActivityResultLauncher)) {
                    avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(fragment.requireContext(), takePhotoActivityResultLauncher)
                }
        }
    }

    private var avatarCameraUri: Uri? = null
    private fun doOpenCamera() {
        avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(activity, takePhotoActivityResultLauncher)
    }
}
