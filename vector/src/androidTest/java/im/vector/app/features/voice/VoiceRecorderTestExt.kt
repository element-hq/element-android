/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
