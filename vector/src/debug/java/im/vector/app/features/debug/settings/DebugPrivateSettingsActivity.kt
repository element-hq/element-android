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

package im.vector.app.features.debug.settings

import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityDebugPrivateSettingsBinding

@AndroidEntryPoint
class DebugPrivateSettingsActivity : VectorBaseActivity<ActivityDebugPrivateSettingsBinding>() {

    private val viewModel: DebugPrivateSettingsViewModel by viewModel()

    override fun getBinding() = ActivityDebugPrivateSettingsBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        views.forceDialPadTabDisplay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handle(DebugPrivateSettingsViewActions.SetDialPadVisibility(isChecked))
        }
    }

    override fun invalidate() = withState(viewModel) {
        views.forceDialPadTabDisplay.isChecked = it.dialPadVisible
    }
}
