/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features

import im.vector.app.config.Config
import im.vector.app.config.OnboardingVariant
import im.vector.app.features.settings.VectorPreferences

interface VectorFeatures {

    fun onboardingVariant(): OnboardingVariant
    fun isOnboardingAlreadyHaveAccountSplashEnabled(): Boolean
    fun isOnboardingSplashCarouselEnabled(): Boolean
    fun isOnboardingUseCaseEnabled(): Boolean
    fun isOnboardingPersonalizeEnabled(): Boolean
    fun isOnboardingCombinedRegisterEnabled(): Boolean
    fun isOnboardingCombinedLoginEnabled(): Boolean
    fun allowExternalUnifiedPushDistributors(): Boolean
    fun isScreenSharingEnabled(): Boolean
    fun isLocationSharingEnabled(): Boolean
    fun forceUsageOfOpusEncoder(): Boolean

    /**
     * This is only to enable if the labs flag should be visible and effective.
     * If on the client-side you want functionality that should be enabled with the new layout,
     * use [VectorPreferences.isNewAppLayoutEnabled] instead.
     */
    fun isNewAppLayoutFeatureEnabled(): Boolean
    fun isVoiceBroadcastEnabled(): Boolean
    fun isUnverifiedSessionsAlertEnabled(): Boolean
}

class DefaultVectorFeatures : VectorFeatures {
    override fun onboardingVariant() = Config.ONBOARDING_VARIANT
    override fun isOnboardingAlreadyHaveAccountSplashEnabled() = true
    override fun isOnboardingSplashCarouselEnabled() = true
    override fun isOnboardingUseCaseEnabled() = true
    override fun isOnboardingPersonalizeEnabled() = true
    override fun isOnboardingCombinedRegisterEnabled() = true
    override fun isOnboardingCombinedLoginEnabled() = true
    override fun allowExternalUnifiedPushDistributors(): Boolean = Config.ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS
    override fun isScreenSharingEnabled(): Boolean = true
    override fun isLocationSharingEnabled() = Config.ENABLE_LOCATION_SHARING
    override fun forceUsageOfOpusEncoder(): Boolean = false
    override fun isNewAppLayoutFeatureEnabled(): Boolean = true
    override fun isVoiceBroadcastEnabled(): Boolean = true
    override fun isUnverifiedSessionsAlertEnabled(): Boolean = true
}
