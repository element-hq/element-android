/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import im.vector.lib.multipicker.entity.MultiPickerImageType
import im.vector.lib.multipicker.utils.MediaType
import im.vector.lib.multipicker.utils.createTemporaryMediaFile
import im.vector.lib.multipicker.utils.toMultiPickerImageType

/**
 * Implementation of taking a photo with Camera.
 */
class CameraPicker {

    /**
     * Start camera by using a ActivityResultLauncher.
     * @return Uri of taken photo or null if the operation is cancelled.
     */
    fun startWithExpectingFile(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>): Uri {
        val photoUri = createPhotoUri(context)
        val intent = createIntent().apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        activityResultLauncher.launch(intent)
        return photoUri
    }

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return Taken photo or null if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user cancelled the operation.
     */
    fun getTakenPhoto(context: Context, photoUri: Uri): MultiPickerImageType? {
        return photoUri.toMultiPickerImageType(context)
    }

    private fun createIntent(): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    }

    companion object {
        fun createPhotoUri(context: Context): Uri {
            val file = createTemporaryMediaFile(context, MediaType.IMAGE)
            val authority = context.packageName + ".multipicker.fileprovider"
            return FileProvider.getUriForFile(context, authority, file)
        }
    }
}
