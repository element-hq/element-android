/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.content.Context
import android.net.Uri
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File

interface VoiceRecorder {
    /**
     * Initialize recording with a pre-recorded file.
     * @param attachmentData data of the recorded file
     */
    fun initializeRecord(attachmentData: ContentAttachmentData)

    /**
     * Start the recording.
     * @param roomId id of the room to start record
     */
    fun startRecord(roomId: String)

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
