/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.dialogs

import android.app.Activity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogPhotoOrVideoBinding
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings

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
            else -> {
                val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_photo_or_video, null)
                val views = DialogPhotoOrVideoBinding.bind(dialogLayout)

                // Show option to set as default in this case
                views.dialogPhotoOrVideoAsDefault.isVisible = true
                // Always default to photo
                views.dialogPhotoOrVideoPhoto.isChecked = true

                MaterialAlertDialogBuilder(activity)
                        .setTitle(CommonStrings.option_take_photo_video)
                        .setView(dialogLayout)
                        .setPositiveButton(CommonStrings._continue) { _, _ ->
                            submit(views, vectorPreferences, listener)
                        }
                        .setNegativeButton(CommonStrings.action_cancel, null)
                        .show()
            }
        }
    }

    private fun submit(
            views: DialogPhotoOrVideoBinding,
            vectorPreferences: VectorPreferences,
            listener: PhotoOrVideoDialogListener
    ) {
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
                .setTitle(CommonStrings.option_take_photo_video)
                .setView(dialogLayout)
                .setPositiveButton(CommonStrings.action_save) { _, _ ->
                    submitSettings(views)
                    listener.onUpdated()
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    private fun submitSettings(views: DialogPhotoOrVideoBinding) {
        vectorPreferences.setTakePhotoVideoMode(
                when {
                    views.dialogPhotoOrVideoPhoto.isChecked -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_PHOTO
                    views.dialogPhotoOrVideoVideo.isChecked -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_VIDEO
                    else -> VectorPreferences.TAKE_PHOTO_VIDEO_MODE_ALWAYS_ASK
                }
        )
    }
}
