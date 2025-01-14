/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import android.content.Context
import android.content.Intent
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAny
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeApplication
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAudio
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeFile
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeText
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo
import javax.inject.Inject

class ShareIntentHandler @Inject constructor(
        private val multiPickerIncomingFiles: MultiPickerIncomingFiles,
        private val context: Context,
) {

    /**
     * This methods aims to handle incoming share intents.
     *
     * @return true if it can handle the intent data, false otherwise
     */
    fun handleIncomingShareIntent(intent: Intent, onFile: (List<ContentAttachmentData>) -> Unit, onPlainText: (String) -> Unit): Boolean {
        val type = intent.resolveType(context) ?: return false
        return when {
            type == MimeTypes.PlainText -> handlePlainText(intent, onPlainText)
            type.isMimeTypeImage() -> onFile(multiPickerIncomingFiles.image(intent)).let { true }
            type.isMimeTypeVideo() -> onFile(multiPickerIncomingFiles.video(intent)).let { true }
            type.isMimeTypeAudio() -> onFile(multiPickerIncomingFiles.audio(intent)).let { true }
            type.isMimeTypeApplication() || type.isMimeTypeFile() || type.isMimeTypeText() || type.isMimeTypeAny() -> {
                onFile(multiPickerIncomingFiles.file(intent)).let { true }
            }
            else -> false
        }
    }

    private fun handlePlainText(intent: Intent, onPlainText: (String) -> Unit): Boolean {
        val content = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        return if (content?.isNotEmpty() == true) {
            onPlainText(content)
            true
        } else {
            false
        }
    }
}
