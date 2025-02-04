/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.entity.MultiPickerVideoType
import im.vector.lib.multipicker.utils.toMultiPickerVideoType

/**
 * Video Picker implementation.
 */
class VideoPicker : Picker<MultiPickerVideoType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected video files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerVideoType> {
        return getSelectedUriList(context, data).mapNotNull { selectedUri ->
            selectedUri.toMultiPickerVideoType(context)
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "video/*"
        }
    }
}
