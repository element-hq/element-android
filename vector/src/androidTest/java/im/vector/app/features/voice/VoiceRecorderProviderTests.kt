/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voice

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.AndroidVersionTestOverrider
import im.vector.app.features.DefaultVectorFeatures
import io.mockk.every
import io.mockk.spyk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.After
import org.junit.Test

class VoiceRecorderProviderTests {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val provider = spyk(VoiceRecorderProvider(context, DefaultVectorFeatures()))

    @After
    fun tearDown() {
        AndroidVersionTestOverrider.restore()
    }

    @Test
    fun provideVoiceRecorderOnAndroidQAndCodecReturnsQRecorder() {
        AndroidVersionTestOverrider.override(Build.VERSION_CODES.Q)
        every { provider.hasOpusEncoder() } returns true
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderQ::class)
    }

    @Test
    fun provideVoiceRecorderOnAndroidQButNoCodecReturnsLRecorder() {
        AndroidVersionTestOverrider.override(Build.VERSION_CODES.Q)
        every { provider.hasOpusEncoder() } returns false
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderL::class)
    }

    @Test
    fun provideVoiceRecorderOnOlderAndroidVersionReturnsLRecorder() {
        AndroidVersionTestOverrider.override(Build.VERSION_CODES.LOLLIPOP)
        provider.provideVoiceRecorder().shouldBeInstanceOf(VoiceRecorderL::class)
    }
}
