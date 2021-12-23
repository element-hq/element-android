/*
 * Copyright 2019 New Vector Ltd
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

import com.airbnb.epoxy.TypedEpoxyController
import javax.inject.Inject
import kotlin.reflect.KClass

data class FeaturesState(
        val features: List<Feature>
)

sealed interface Feature {

    data class EnumFeature<T : Enum<T>>(
            val label: String,
            val selection: T?,
            val default: T,
            val options: List<T>,
            val type: KClass<T>
    ) : Feature
}

class FeaturesController @Inject constructor() : TypedEpoxyController<FeaturesState>() {

    var listener: EnumFeatureItem.Listener? = null

    override fun buildModels(data: FeaturesState?) {
        if (data == null) return

        data.features.forEachIndexed { index, feature ->
            when (feature) {
                is Feature.EnumFeature<*> -> enumFeatureItem {
                    id(index)
                    feature(feature)
                    listener(this@FeaturesController.listener)
                }
            }
        }
    }
}
