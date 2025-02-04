/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.config.Config
import im.vector.app.test.fakes.FakeClock
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorPreferences
import org.amshove.kluent.shouldBe
import org.junit.Test

private val AN_EPOCH = Config.SHOW_UNVERIFIED_SESSIONS_ALERT_AFTER_MILLIS.toLong()
private const val A_DEVICE_ID = "A_DEVICE_ID"

class ShouldShowUnverifiedSessionsAlertUseCaseTest {

    private val fakeVectorFeatures = FakeVectorFeatures()
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeClock = FakeClock()

    private val shouldShowUnverifiedSessionsAlertUseCase = ShouldShowUnverifiedSessionsAlertUseCase(
            vectorFeatures = fakeVectorFeatures,
            vectorPreferences = fakeVectorPreferences.instance,
            clock = fakeClock,
    )

    @Test
    fun `given the feature is disabled then the use case returns false`() {
        fakeVectorFeatures.givenUnverifiedSessionsAlertEnabled(false)
        fakeVectorPreferences.givenUnverifiedSessionsAlertLastShownMillis(0L)

        shouldShowUnverifiedSessionsAlertUseCase.execute(A_DEVICE_ID) shouldBe false
    }

    @Test
    fun `given the feature in enabled and there is not a saved preference then the use case returns true`() {
        fakeVectorFeatures.givenUnverifiedSessionsAlertEnabled(true)
        fakeVectorPreferences.givenUnverifiedSessionsAlertLastShownMillis(0L)
        fakeClock.givenEpoch(AN_EPOCH + 1)

        shouldShowUnverifiedSessionsAlertUseCase.execute(A_DEVICE_ID) shouldBe true
    }

    @Test
    fun `given the feature in enabled and last shown is a long time ago then the use case returns true`() {
        fakeVectorFeatures.givenUnverifiedSessionsAlertEnabled(true)
        fakeVectorPreferences.givenUnverifiedSessionsAlertLastShownMillis(AN_EPOCH)
        fakeClock.givenEpoch(AN_EPOCH * 2 + 1)

        shouldShowUnverifiedSessionsAlertUseCase.execute(A_DEVICE_ID) shouldBe true
    }

    @Test
    fun `given the feature in enabled and last shown is not a long time ago then the use case returns false`() {
        fakeVectorFeatures.givenUnverifiedSessionsAlertEnabled(true)
        fakeVectorPreferences.givenUnverifiedSessionsAlertLastShownMillis(AN_EPOCH)
        fakeClock.givenEpoch(AN_EPOCH + 1)

        shouldShowUnverifiedSessionsAlertUseCase.execute(A_DEVICE_ID) shouldBe false
    }
}
