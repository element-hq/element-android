/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
