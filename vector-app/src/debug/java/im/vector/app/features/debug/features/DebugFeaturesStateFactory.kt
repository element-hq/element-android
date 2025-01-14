/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.features

import androidx.datastore.preferences.core.Preferences
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import javax.inject.Inject
import kotlin.reflect.KFunction1

class DebugFeaturesStateFactory @Inject constructor(
        private val debugFeatures: DebugVectorFeatures,
        private val defaultFeatures: DefaultVectorFeatures
) {

    fun create(): FeaturesState {
        return FeaturesState(
                listOf(
                        createEnumFeature(
                                label = "Onboarding variant",
                                featureOverride = debugFeatures.onboardingVariant(),
                                featureDefault = defaultFeatures.onboardingVariant()
                        ),
                        createBooleanFeature(
                                label = "FTUE Splash - I already have an account",
                                key = DebugFeatureKeys.onboardingAlreadyHaveAnAccount,
                                factory = VectorFeatures::isOnboardingAlreadyHaveAccountSplashEnabled
                        ),
                        createBooleanFeature(
                                label = "FTUE Splash - carousel",
                                key = DebugFeatureKeys.onboardingSplashCarousel,
                                factory = VectorFeatures::isOnboardingSplashCarouselEnabled
                        ),
                        createBooleanFeature(
                                label = "FTUE Use Case",
                                key = DebugFeatureKeys.onboardingUseCase,
                                factory = VectorFeatures::isOnboardingUseCaseEnabled
                        ),
                        createBooleanFeature(
                                label = "FTUE Personalize profile",
                                key = DebugFeatureKeys.onboardingPersonalize,
                                factory = VectorFeatures::isOnboardingPersonalizeEnabled
                        ),
                        createBooleanFeature(
                                label = "FTUE Combined register",
                                key = DebugFeatureKeys.onboardingCombinedRegister,
                                factory = VectorFeatures::isOnboardingCombinedRegisterEnabled
                        ),
                        createBooleanFeature(
                                label = "FTUE Combined login",
                                key = DebugFeatureKeys.onboardingCombinedLogin,
                                factory = VectorFeatures::isOnboardingCombinedLoginEnabled
                        ),
                        createBooleanFeature(
                                label = "Allow external UnifiedPush distributors",
                                key = DebugFeatureKeys.allowExternalUnifiedPushDistributors,
                                factory = VectorFeatures::allowExternalUnifiedPushDistributors
                        ),
                        createBooleanFeature(
                                label = "Enable Live Location Sharing",
                                key = DebugFeatureKeys.liveLocationSharing,
                                factory = VectorFeatures::isLocationSharingEnabled
                        ),
                        createBooleanFeature(
                                label = "Force usage of OpusEncoder library",
                                key = DebugFeatureKeys.forceUsageOfOpusEncoder,
                                factory = VectorFeatures::forceUsageOfOpusEncoder
                        ),
                        createBooleanFeature(
                                label = "Enable New App Layout",
                                key = DebugFeatureKeys.newAppLayoutEnabled,
                                factory = VectorFeatures::isNewAppLayoutFeatureEnabled
                        ),
                        createBooleanFeature(
                                label = "Enable Voice Broadcast",
                                key = DebugFeatureKeys.voiceBroadcastEnabled,
                                factory = VectorFeatures::isVoiceBroadcastEnabled
                        ),
                )
        )
    }

    private fun createBooleanFeature(key: Preferences.Key<Boolean>, label: String, factory: KFunction1<VectorFeatures, Boolean>): Feature {
        return Feature.BooleanFeature(
                label = label,
                featureOverride = factory.invoke(debugFeatures).takeIf { debugFeatures.hasOverride(key) },
                featureDefault = factory.invoke(defaultFeatures),
                key = key
        )
    }

    private inline fun <reified T : Enum<T>> createEnumFeature(label: String, featureOverride: T, featureDefault: T): Feature {
        return Feature.EnumFeature(
                label = label,
                override = featureOverride.takeIf { debugFeatures.hasEnumOverride(T::class) },
                default = featureDefault,
                options = enumValues<T>().toList(),
                type = T::class
        )
    }
}
