/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import android.content.Context
import android.content.Intent
import im.vector.lib.multipicker.MultiPicker
import javax.inject.Inject

class MultiPickerIncomingFiles @Inject constructor(
        private val context: Context,
) {

    fun image(intent: Intent) = MultiPicker.get(MultiPicker.IMAGE).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun video(intent: Intent) = MultiPicker.get(MultiPicker.VIDEO).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun media(intent: Intent) = MultiPicker.get(MultiPicker.MEDIA).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun file(intent: Intent) = MultiPicker.get(MultiPicker.FILE).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }

    fun audio(intent: Intent) = MultiPicker.get(MultiPicker.AUDIO).getIncomingFiles(context, intent).map { it.toContentAttachmentData() }
}
