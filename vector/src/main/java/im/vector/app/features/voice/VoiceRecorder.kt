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
import android.net.Uri
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File

interface VoiceRecorder {

    /**
     * Audio file extension (eg. `mp4`).
     */
    val fileNameExt: String

    /**
     * Initialize recording with an optional pre-recorded file.
     *
     * @param roomId id of the room to initialize record
     * @param attachmentData data of the pre-recorded file, if any.
     */
    fun initializeRecord(roomId: String, attachmentData: ContentAttachmentData? = null)

    /**
     * Start the recording.
     * @param roomId id of the room to start record
     */
    fun startRecord(roomId: String)

    /**
     * Pause the recording.
     */
    fun pauseRecord()

    /**
     * Resume the recording.
     */
    fun resumeRecord()

    /**
     * Stop the recording.
     */
    fun stopRecord()

    /**
     * Remove the file.
     */
    fun cancelRecord()

    fun getMaxAmplitude(): Int

    /**
     * Guaranteed to be a ogg file.
     */
    fun getVoiceMessageFile(): File?
}

/**
 * Ensures a voice records directory exists and returns it.
 */
internal fun VoiceRecorder.ensureAudioDirectory(context: Context): File {
    return File(context.cacheDir, "voice_records").also {
        it.mkdirs()
    }
}

internal fun ContentAttachmentData.findVoiceFile(baseDirectory: File): File {
    return File(baseDirectory, queryUri.takePathAfter(baseDirectory.name))
}

private fun Uri.takePathAfter(after: String): String {
    return pathSegments.takeLastWhile { it != after }.joinToString(separator = "/") { it }
}
