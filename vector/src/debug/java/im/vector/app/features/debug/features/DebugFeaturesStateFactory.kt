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

package im.vector.app.features.debug.features

import im.vector.app.features.DefaultVectorFeatures
import javax.inject.Inject

class DebugFeaturesStateFactory @Inject constructor(
        private val debugFeatures: DebugVectorFeatures,
        private val defaultFeatures: DefaultVectorFeatures
) {

    fun create(): FeaturesState {
        return FeaturesState(listOf(
                createEnumFeature(
                        label = "Onboarding variant",
                        featureOverride = debugFeatures.onboardingVariant(),
                        featureDefault = defaultFeatures.onboardingVariant()
                ),

                Feature.BooleanFeature(
                        label = "FTUE Splash - I already have an account",
                        featureOverride = debugFeatures.isAlreadyHaveAccountSplashEnabled().takeIf {
                            debugFeatures.hasOverride(DebugFeatureKeys.alreadyHaveAnAccount)
                        },
                        featureDefault = defaultFeatures.isAlreadyHaveAccountSplashEnabled(),
                        key = DebugFeatureKeys.alreadyHaveAnAccount
                )
        ))
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
