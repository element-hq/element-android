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

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import javax.inject.Inject

@AndroidEntryPoint
class DebugFeaturesSettingsActivity : VectorBaseActivity<FragmentGenericRecyclerBinding>() {

    @Inject lateinit var debugFeatures: DebugVectorFeatures
    @Inject lateinit var debugFeaturesStateFactory: DebugFeaturesStateFactory
    @Inject lateinit var controller: FeaturesController

    override fun getBinding() = FragmentGenericRecyclerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.listener = object : FeaturesController.Listener {
            override fun <T : Enum<T>> onEnumOptionSelected(option: T?, feature: Feature.EnumFeature<T>) {
                debugFeatures.overrideEnum(option, feature.type)
            }

            override fun onBooleanOptionSelected(option: Boolean?, feature: Feature.BooleanFeature) {
                debugFeatures.override(option, feature.key)
            }
        }
        views.genericRecyclerView.configureWith(controller)
        controller.setData(debugFeaturesStateFactory.create())
    }

    override fun onDestroy() {
        controller.listener = null
        views.genericRecyclerView.cleanup()
        super.onDestroy()
    }
}
