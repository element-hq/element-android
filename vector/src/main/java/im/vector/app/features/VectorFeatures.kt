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

package im.vector.app.features

import im.vector.app.BuildConfig

interface VectorFeatures {

    fun onboardingVariant(): OnboardingVariant
    fun isOnboardingAlreadyHaveAccountSplashEnabled(): Boolean
    fun isOnboardingSplashCarouselEnabled(): Boolean
    fun isOnboardingUseCaseEnabled(): Boolean
    fun isOnboardingPersonalizeEnabled(): Boolean
    fun isOnboardingCombinedRegisterEnabled(): Boolean
    fun isLiveLocationEnabled(): Boolean

    enum class OnboardingVariant {
        LEGACY,
        LOGIN_2,
        FTUE_AUTH
    }
}

class DefaultVectorFeatures : VectorFeatures {
    override fun onboardingVariant(): VectorFeatures.OnboardingVariant = BuildConfig.ONBOARDING_VARIANT
    override fun isOnboardingAlreadyHaveAccountSplashEnabled() = true
    override fun isOnboardingSplashCarouselEnabled() = true
    override fun isOnboardingUseCaseEnabled() = true
    override fun isOnboardingPersonalizeEnabled() = false
    override fun isOnboardingCombinedRegisterEnabled() = false
    override fun isLiveLocationEnabled(): Boolean = false
}
