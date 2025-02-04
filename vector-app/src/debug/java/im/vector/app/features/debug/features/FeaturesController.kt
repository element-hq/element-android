/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.features

import androidx.datastore.preferences.core.Preferences
import com.airbnb.epoxy.TypedEpoxyController
import javax.inject.Inject
import kotlin.reflect.KClass

data class FeaturesState(
        val features: List<Feature>
)

sealed interface Feature {

    data class EnumFeature<T : Enum<T>>(
            val label: String,
            val override: T?,
            val default: T,
            val options: List<T>,
            val type: KClass<T>
    ) : Feature

    data class BooleanFeature(
            val label: String,
            val featureOverride: Boolean?,
            val featureDefault: Boolean,
            val key: Preferences.Key<Boolean>
    ) : Feature
}

class FeaturesController @Inject constructor() : TypedEpoxyController<FeaturesState>() {

    var listener: Listener? = null

    override fun buildModels(data: FeaturesState?) {
        if (data == null) return

        data.features.forEachIndexed { index, feature ->
            when (feature) {
                is Feature.EnumFeature<*> -> enumFeatureItem {
                    id(index)
                    feature(feature)
                    listener(this@FeaturesController.listener)
                }
                is Feature.BooleanFeature -> booleanFeatureItem {
                    id(index)
                    feature(feature)
                    listener(this@FeaturesController.listener)
                }
            }
        }
    }

    interface Listener : EnumFeatureItem.Listener, BooleanFeatureItem.Listener
}
