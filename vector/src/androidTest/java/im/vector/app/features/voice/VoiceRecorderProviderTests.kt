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

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.TestBuildVersionSdkIntProvider
import im.vector.app.features.DefaultVectorFeatures
import io.mockk.every
import io.mockk.spyk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test

class VoiceRecorderProviderTests {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val buildVersionSdkIntProvider = TestBuildVersionSdkIntProvider()
    private val provider = spyk(VoiceRecorderProvider(context, DefaultVectorFeatures(), buildVersionSdkIntProvider))

    @Test
    fun provideVoiceRecorderOnAndroidQAndCodecReturnsQRecorder() {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.Q
        every { provider.hasOpusEncoder() } returns true
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderQ::class)
    }

    @Test
    fun provideVoiceRecorderOnAndroidQButNoCodecReturnsLRecorder() {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.Q
        every { provider.hasOpusEncoder() } returns false
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderL::class)
    }

    @Test
    fun provideVoiceRecorderOnOlderAndroidVersionReturnsLRecorder() {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderL::class)
    }
}
