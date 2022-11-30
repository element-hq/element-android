/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
