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
package im.vector.riotx.features.attachments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.kbeanie.multipicker.api.CameraImagePicker
import com.kbeanie.multipicker.api.FilePicker
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.Picker.*
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.entity.ChosenFile
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.kbeanie.multipicker.api.entity.ChosenVideo
import com.kbeanie.multipicker.core.PickerManager
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.core.platform.Restorable

private const val CAPTURE_PATH_KEY = "CAPTURE_PATH_KEY"

class AttachmentsHelper(private val fragment: Fragment, private val callback: Callback) : Restorable {

    interface Callback {
        fun onAttachmentsReady(attachments: List<ContentAttachmentData>)
        fun onAttachmentsProcessFailed()
    }

    private val attachmentsPickerCallback = AttachmentsPickerCallback(callback)

    private val imagePicker by lazy {
        ImagePicker(fragment).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
            it.allowMultiple()
        }
    }

    private val cameraImagePicker by lazy {
        CameraImagePicker(fragment).also {
            it.setImagePickerCallback(attachmentsPickerCallback)
        }
    }

    private val filePicker by lazy {
        FilePicker(fragment).also {
            it.allowMultiple()
            it.setFilePickerCallback(attachmentsPickerCallback)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capturePath?.also {
            outState.putString(CAPTURE_PATH_KEY, it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        capturePath = savedInstanceState?.getString(CAPTURE_PATH_KEY)
    }

    var capturePath: String? = null
        private set

    fun selectFile() {
        filePicker.pickFile()
    }

    fun selectGallery() {
        imagePicker.pickImage()
    }

    fun openCamera() {
        capturePath = cameraImagePicker.pickImage()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            val pickerManager = getPickerManager(requestCode)
            if (pickerManager != null) {
                pickerManager.submit(data)
                return true
            }
        }
        return false
    }

    private fun getPickerManager(requestCode: Int): PickerManager? {
        return when (requestCode) {
            PICK_IMAGE_DEVICE -> imagePicker
            PICK_IMAGE_CAMERA -> cameraImagePicker
            PICK_FILE         -> filePicker
            else              -> null
        }
    }

    private inner class AttachmentsPickerCallback(private val callback: Callback) : ImagePickerCallback, FilePickerCallback {

        override fun onFilesChosen(files: MutableList<ChosenFile>?) {
            if (files.isNullOrEmpty()) {
                callback.onAttachmentsProcessFailed()
            } else {
                val attachments = files.map {
                    it.toContentAttachmentData()
                }
                callback.onAttachmentsReady(attachments)
            }
        }

        override fun onImagesChosen(images: MutableList<ChosenImage>?) {
            if (images.isNullOrEmpty()) {
                callback.onAttachmentsProcessFailed()
            } else {
                val attachments = images.map {
                    it.toContentAttachmentData()
                }
                callback.onAttachmentsReady(attachments)
            }
        }

        override fun onError(error: String?) {
            callback.onAttachmentsProcessFailed()
        }
    }


    private fun ChosenFile.toContentAttachmentData(): ContentAttachmentData {
        return ContentAttachmentData(
                path = originalPath,
                mimeType = mimeType,
                type = mapType(),
                size = size,
                date = createdAt.time,
                name = displayName
        )
    }

    private fun ChosenFile.mapType(): ContentAttachmentData.Type {
        return when {
            mimeType.startsWith("image/") -> ContentAttachmentData.Type.IMAGE
            mimeType.startsWith("video/") -> ContentAttachmentData.Type.VIDEO
            mimeType.startsWith("audio/") -> ContentAttachmentData.Type.AUDIO
            else                          -> ContentAttachmentData.Type.FILE
        }
    }

    private fun ChosenImage.toContentAttachmentData(): ContentAttachmentData {
        return ContentAttachmentData(
                path = originalPath,
                mimeType = mimeType,
                type = mapType(),
                name = displayName,
                size = size,
                height = height.toLong(),
                width = width.toLong(),
                date = createdAt.time
        )
    }

    private fun ChosenVideo.toContentAttachmentData(): ContentAttachmentData {
        return ContentAttachmentData(
                path = originalPath,
                mimeType = mimeType,
                type = ContentAttachmentData.Type.VIDEO,
                size = size,
                date = createdAt.time,
                height = height.toLong(),
                width = width.toLong(),
                duration = duration,
                name = displayName
        )
    }

}