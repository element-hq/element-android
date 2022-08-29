/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSettingsSessionOverviewBinding

/**
 * Display the overview info about a Session.
 */
@AndroidEntryPoint
class SessionOverviewFragment :
        VectorBaseFragment<FragmentSettingsSessionOverviewBinding>() {

    private val viewModel: SessionOverviewViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsSessionOverviewBinding {
        return FragmentSettingsSessionOverviewBinding.inflate(inflater, container, false)
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateToolbar(state.isCurrentSession)
    }

    private fun updateToolbar(isCurrentSession: Boolean) {
        val titleResId = if (isCurrentSession) R.string.device_manager_current_session_title else R.string.device_manager_session_title
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(titleResId)
    }
}
