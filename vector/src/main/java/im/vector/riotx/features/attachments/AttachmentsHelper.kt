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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import im.vector.riotx.BuildConfig
import im.vector.riotx.core.resources.MIME_TYPE_ALL_CONTENT
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class AttachmentsHelper(private val context: Context) {

    private var capturePath: String? = null

    fun selectFile(fragment: Fragment, requestCode: Int) {
        selectMediaType(fragment, "*/*", null, requestCode)
    }

    fun selectGallery(fragment: Fragment, requestCode: Int) {
        selectMediaType(fragment, "image/*", arrayOf("image/*", "video/*"), requestCode)
    }

    fun openCamera(fragment: Fragment, requestCode: Int) {
        dispatchTakePictureIntent(fragment, requestCode)
    }


    fun handleOpenCameraResult(): List<Attachment> {
        val attachment = getAttachmentFromContentResolver(Uri.parse(capturePath))
        return if (attachment == null) {
            emptyList()
        } else {
            listOf(attachment)
        }
    }

    fun handleSelectResult(data: Intent?): List<Attachment> {
        val clipData = data?.clipData
        if (clipData != null) {
            return (0 until clipData.itemCount).map {
                clipData.getItemAt(it)
            }.mapNotNull {
                getAttachmentFromContentResolver(it.uri)
            }
        } else {
            val uri = data?.data ?: return emptyList()
            val attachment = getAttachmentFromContentResolver(uri)
            return if (attachment == null) {
                emptyList()
            } else {
                listOf(attachment)
            }
        }
    }

    private fun selectMediaType(fragment: Fragment, type: String, extraMimeType: Array<String>?, requestCode: Int) {
        val intent = Intent()
        intent.type = type
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        if (extraMimeType != null) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType)
        }
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        try {
            fragment.startActivityForResult(intent, requestCode)
            return
        } catch (exception: ActivityNotFoundException) {
            Timber.e(exception)
        }
        intent.action = Intent.ACTION_GET_CONTENT
        try {
            fragment.startActivityForResult(intent, requestCode)
        } catch (exception: ActivityNotFoundException) {
            Timber.e(exception)
        }
    }

    private fun getAttachmentFromContentResolver(uri: Uri): Attachment? {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val fileName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                val fileSize = it.getLong(it.getColumnIndex(OpenableColumns.SIZE))
                val mimeType = context.contentResolver.getType(uri) ?: MIME_TYPE_ALL_CONTENT
                Attachment(uri.toString(), mimeType, fileName, fileSize)
            } else {
                null
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        capturePath = image.absolutePath
        return image
    }

    private fun dispatchTakePictureIntent(fragment: Fragment, requestCode: Int) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(fragment.requireActivity().packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile(fragment.requireContext())
            } catch (ex: IOException) {
                Timber.e(ex, "Couldn't create image file")
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(fragment.requireContext(), BuildConfig.APPLICATION_ID + ".fileProvider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                fragment.startActivityForResult(takePictureIntent, requestCode)
            }
        }
    }

}