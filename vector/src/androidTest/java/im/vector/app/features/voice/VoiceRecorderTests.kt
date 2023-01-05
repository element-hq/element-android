/*
 * Copyright (c) 2022 New Vector Ltd
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
