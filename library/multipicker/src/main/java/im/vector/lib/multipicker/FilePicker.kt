/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import im.vector.lib.multipicker.entity.MultiPickerBaseType
import im.vector.lib.multipicker.entity.MultiPickerFileType
import im.vector.lib.multipicker.utils.getColumnIndexOrNull
import im.vector.lib.multipicker.utils.isMimeTypeAudio
import im.vector.lib.multipicker.utils.isMimeTypeImage
import im.vector.lib.multipicker.utils.isMimeTypeVideo
import im.vector.lib.multipicker.utils.toMultiPickerAudioType
import im.vector.lib.multipicker.utils.toMultiPickerImageType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType

/**
 * Implementation of selecting any type of files.
 */
class FilePicker : Picker<MultiPickerBaseType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerBaseType> {
        return getSelectedUriList(context, data).mapNotNull { selectedUri ->
            val type = context.contentResolver.getType(selectedUri)

            when {
                type.isMimeTypeVideo() -> selectedUri.toMultiPickerVideoType(context)
                type.isMimeTypeImage() -> selectedUri.toMultiPickerImageType(context)
                type.isMimeTypeAudio() -> selectedUri.toMultiPickerAudioType(context)
                else -> {
                    // Other files
                    context.contentResolver.query(selectedUri, null, null, null, null)
                            ?.use { cursor ->
                                val nameColumn = cursor.getColumnIndexOrNull(OpenableColumns.DISPLAY_NAME) ?: return@use null
                                val sizeColumn = cursor.getColumnIndexOrNull(OpenableColumns.SIZE) ?: return@use null
                                if (cursor.moveToFirst()) {
                                    val name = cursor.getStringOrNull(nameColumn)
                                    val size = cursor.getLongOrNull(sizeColumn) ?: 0

                                    MultiPickerFileType(
                                            name,
                                            size,
                                            context.contentResolver.getType(selectedUri),
                                            selectedUri
                                    )
                                } else {
                                    null
                                }
                            }
                }
            }
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "*/*"
        }
    }
}
