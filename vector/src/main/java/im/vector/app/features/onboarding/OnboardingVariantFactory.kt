/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.onboarding

import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.login2.LoginViewModel2
import im.vector.app.features.onboarding.ftueauth.FtueAuthVariant
import javax.inject.Inject

class OnboardingVariantFactory @Inject constructor(
        private val vectorFeatures: VectorFeatures,
        private val orientationLocker: ScreenOrientationLocker,
) {

    fun create(activity: OnboardingActivity,
               views: ActivityLoginBinding,
               onboardingViewModel: Lazy<OnboardingViewModel>,
               loginViewModel2: Lazy<LoginViewModel2>
    ) = when (vectorFeatures.onboardingVariant()) {
        VectorFeatures.OnboardingVariant.LEGACY    -> error("Legacy is not supported by the FTUE")
        VectorFeatures.OnboardingVariant.FTUE_AUTH -> FtueAuthVariant(
                views = views,
                onboardingViewModel = onboardingViewModel.value,
                activity = activity,
                supportFragmentManager = activity.supportFragmentManager,
                vectorFeatures = vectorFeatures,
                orientationLocker = orientationLocker
        )
        VectorFeatures.OnboardingVariant.LOGIN_2   -> Login2Variant(
                views = views,
                loginViewModel = loginViewModel2.value,
                activity = activity,
                supportFragmentManager = activity.supportFragmentManager
        )
    }
}
