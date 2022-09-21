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

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldExist
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotExist
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.JVM)
class VoiceRecorderLTests {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recorder = spyk(VoiceRecorderL(context, Dispatchers.IO))

    @Test
    fun startRecordCreatesOggFile() = with(recorder) {
        getVoiceMessageFile().shouldBeNull()

        startRecord("some_room_id")
        runBlocking { waitUntilRecordingFileExists() }

        stopRecord()
    }

    // Renamed to be run first... It fixes it.
    @Test
    fun atFirstStopRecordKeepsFile() = with(recorder) {
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
