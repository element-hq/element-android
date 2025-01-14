/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.entity.MultiPickerAudioType
import im.vector.lib.multipicker.utils.toMultiPickerAudioType

/**
 * Audio file picker implementation.
 */
class AudioPicker : Picker<MultiPickerAudioType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected audio files or empty list if user did not select any files.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerAudioType> {
        return getSelectedUriList(context, data).mapNotNull { selectedUri ->
            selectedUri.toMultiPickerAudioType(context)
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = "audio/*"
        }
    }
}
