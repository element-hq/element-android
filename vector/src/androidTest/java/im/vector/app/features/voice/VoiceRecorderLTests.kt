/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldExist
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotExist
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File

@Ignore("Disabled temporarily so that we can unblock other PRs.")
class VoiceRecorderLTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recorder = VoiceRecorderL(context, Dispatchers.IO, createFakeOpusEncoder())

    @Test
    fun startRecordCreatesOggFile() = with(recorder) {
        getVoiceMessageFile().shouldBeNull()

        startRecord("some_room_id")
        runBlocking { waitUntilRecordingFileExists() }

        stopRecord()
    }

    @Test
    fun stopRecordKeepsFile() = with(recorder) {
        getVoiceMessageFile().shouldBeNull()

        startRecord("some_room_id")
        runBlocking { waitUntilRecordingFileExists() }
        stopRecord()

        getVoiceMessageFile().shouldNotBeNullAndExist()
    }

    @Test
    fun cancelRecordRemovesFile() = with(recorder) {
        startRecord("some_room_id")
        val file = runBlocking { waitUntilRecordingFileExists() }

        cancelRecord()

        getVoiceMessageFile().shouldBeNull()
        file!!.shouldNotExist()
    }
}

private fun File?.shouldNotBeNullAndExist() {
    shouldNotBeNull()
    shouldExist()
}
