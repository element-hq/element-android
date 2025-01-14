/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldExist
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File

class VoiceRecorderTests {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val voiceRecorder = VoiceRecorderL(context, Dispatchers.IO, createFakeOpusEncoder())
    private val audioDirectory = File(context.cacheDir, "voice_records")

    @After
    fun tearDown() {
        audioDirectory.deleteRecursively()
    }

    @Test
    fun ensureAudioDirectoryCreatesIt() {
        voiceRecorder.ensureAudioDirectory(context)
        audioDirectory.shouldExist()
    }

    @Test
    fun findVoiceFileSearchesInDirectory() {
        val filename = "someFile.ogg"
        val attachment = ContentAttachmentData(
                queryUri = Uri.parse(filename),
                mimeType = "ogg",
                type = ContentAttachmentData.Type.AUDIO
        )
        attachment.findVoiceFile(audioDirectory) shouldBeEqualTo File(audioDirectory, filename)
    }
}
