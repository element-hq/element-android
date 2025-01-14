/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import io.mockk.every
import io.mockk.spyk

class FakeVectorFeatures : VectorFeatures by spyk<DefaultVectorFeatures>() {

    fun givenPersonalisationEnabled() {
        every { isOnboardingPersonalizeEnabled() } returns true
    }

    fun givenCombinedRegisterEnabled() {
        every { isOnboardingCombinedRegisterEnabled() } returns true
    }

    fun givenCombinedLoginEnabled() {
        every { isOnboardingCombinedLoginEnabled() } returns true
    }

    fun givenOnboardingUseCaseEnabled() {
        every { isOnboardingUseCaseEnabled() } returns true
    }

    fun givenCombinedLoginDisabled() {
        every { isOnboardingCombinedLoginEnabled() } returns false
    }

    fun givenLocationSharing(isEnabled: Boolean) {
        every { isLocationSharingEnabled() } returns isEnabled
    }

    fun givenVoiceBroadcast(isEnabled: Boolean) {
        every { isVoiceBroadcastEnabled() } returns isEnabled
    }

    fun givenUnverifiedSessionsAlertEnabled(isEnabled: Boolean) {
        every { isUnverifiedSessionsAlertEnabled() } returns isEnabled
    }

    fun givenExternalDistributorsAreAllowed(allowed: Boolean) {
        every { allowExternalUnifiedPushDistributors() } returns allowed
    }
}
