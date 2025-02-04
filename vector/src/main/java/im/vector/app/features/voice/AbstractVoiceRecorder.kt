/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.content.Context
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.md5
import java.io.File
import java.util.UUID

abstract class AbstractVoiceRecorder(
        private val context: Context,
) : VoiceRecorder {

    private val outputDirectory: File by lazy { ensureAudioDirectory(context) }
    protected var outputFile: File? = null

    override fun initializeRecord(roomId: String, attachmentData: ContentAttachmentData?) {
        if (attachmentData != null) {
            outputFile = attachmentData.findVoiceFile(outputDirectory)
        }
    }

    override fun cancelRecord() {
        stopRecord()

        outputFile?.delete()
        outputFile = null
    }

    override fun getVoiceMessageFile(): File? {
        return outputFile
    }

    protected fun createOutputFile(roomId: String): File {
        val fileName = "${UUID.randomUUID()}.$fileNameExt"
        val outputDirectoryForRoom = File(outputDirectory, roomId.md5()).apply {
            mkdirs()
        }
        return File(outputDirectoryForRoom, fileName)
    }
}
