/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.config.OnboardingVariant
import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.core.resources.BuildMeta
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.ftueauth.FtueAuthVariant
import javax.inject.Inject

class OnboardingVariantFactory @Inject constructor(
        private val vectorFeatures: VectorFeatures,
        private val orientationLocker: ScreenOrientationLocker,
        private val buildMeta: BuildMeta,
) {

    fun create(
            activity: OnboardingActivity,
            views: ActivityLoginBinding,
            onboardingViewModel: Lazy<OnboardingViewModel>,
    ) = when (vectorFeatures.onboardingVariant()) {
        OnboardingVariant.LEGACY -> error("Legacy is not supported by the FTUE")
        OnboardingVariant.FTUE_AUTH -> FtueAuthVariant(
                views = views,
                onboardingViewModel = onboardingViewModel.value,
                activity = activity,
                supportFragmentManager = activity.supportFragmentManager,
                vectorFeatures = vectorFeatures,
                orientationLocker = orientationLocker,
                buildMeta = buildMeta,
        )
    }
}
