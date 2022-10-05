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

import im.vector.app.core.utils.waitUntil
import im.vector.app.test.fakes.FakeOggOpusEncoder
import org.amshove.kluent.shouldExist
import org.amshove.kluent.shouldNotBeNull
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Give voice recorders some time to start recording and create the audio file
suspend fun VoiceRecorder.waitUntilRecordingFileExists(timeout: Duration = 1.seconds, delay: Duration = 10.milliseconds): File? {
    waitUntil(timeout = timeout, retryDelay = delay) {
        getVoiceMessageFile().run {
            shouldNotBeNull()
            shouldExist()
        }
    }
    return getVoiceMessageFile()
}

internal fun createFakeOpusEncoder() = FakeOggOpusEncoder().apply { createEmptyFileOnInit() }
