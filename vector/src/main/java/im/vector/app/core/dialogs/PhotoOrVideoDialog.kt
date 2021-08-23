/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.core.dialogs

import android.app.Activity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogPhotoOrVideoBinding
import im.vector.app.features.settings.VectorPreferences

class PhotoOrVideoDialog(
        private val activity: Activity,
        private val vectorPreferences: VectorPreferences
) {

    interface PhotoOrVideoDialogListener {
        fun takePhoto()
        fun takeVideo()
    }

    interface PhotoOrVideoDialogSettingsListener {
        fun onUpdated()
    }

    fun show(listener: PhotoOrVideoDialogListener) {
        when (vectorPreferences.getTakePhotoVideoMode()) {
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO -> listener.takePhoto()
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO -> listener.takeVideo()
            /* VectorPreferences.TAKE_PHOTO_VIDEO_MODE_ALWAYS_ASK */
            else                                          -> {
                val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_photo_or_video, null)
                val views = DialogPhotoOrVideoBinding.bind(dialogLayout)

                // Show option to set as default in this case
                views.dialogPhotoOrVideoAsDefault.isVisible = true
                // Always default to photo
                views.dialogPhotoOrVideoPhoto.isChecked = true

                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.option_take_photo_video)
                        .setView(dialogLayout)
                        .setPositiveButton(R.string._continue) { _, _ ->
                            submit(views, vectorPreferences, listener)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }
        }
    }

    private fun submit(views: DialogPhotoOrVideoBinding,
                       vectorPreferences: VectorPreferences,
                       listener: PhotoOrVideoDialogListener) {
        val mode = if (views.dialogPhotoOrVideoPhoto.isChecked) {
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO
        } else {
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO
        }

        if (views.dialogPhotoOrVideoAsDefault.isChecked) {
            vectorPreferences.setTakePhotoVideoMode(mode)
        }

        when (mode) {
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO -> listener.takePhoto()
            VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO -> listener.takeVideo()
        }
    }

    fun showForSettings(listener: PhotoOrVideoDialogSettingsListener) {
        val currentMode = vectorPreferences.getTakePhotoVideoMode()

        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_photo_or_video, null)
        val views = DialogPhotoOrVideoBinding.bind(dialogLayout)

        // Show option for always ask in this case
        views.dialogPhotoOrVideoAlwaysAsk.isVisible = true
        // Always default to photo
        views.dialogPhotoOrVideoPhoto.isChecked = currentMode == VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO
        views.dialogPhotoOrVideoVideo.isChecked = currentMode == VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO
        views.dialogPhotoOrVideoAlwaysAsk.isChecked = currentMode == VectorPreferences.TAKE_PHOTO_VIDEO_MODE_ALWAYS_ASK

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.option_take_photo_video)
                .setView(dialogLayout)
                .setPositiveButton(R.string.save) { _, _ ->
                    submitSettings(views)
                    listener.onUpdated()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun submitSettings(views: DialogPhotoOrVideoBinding) {
        vectorPreferences.setTakePhotoVideoMode(
                when {
                    views.dialogPhotoOrVideoPhoto.isChecked -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO
                    views.dialogPhotoOrVideoVideo.isChecked -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO
                    else                                    -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_ALWAYS_ASK
                }
        )
    }
}
