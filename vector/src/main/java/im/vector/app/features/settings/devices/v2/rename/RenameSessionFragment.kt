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

package im.vector.app.features.settings.devices.v2.rename

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSessionRenameBinding
import javax.inject.Inject

/**
 * Display the screen to rename a Session.
 */
@AndroidEntryPoint
class RenameSessionFragment :
        VectorBaseFragment<FragmentSessionRenameBinding>() {

    private val viewModel: RenameSessionViewModel by fragmentViewModel()

    @Inject lateinit var viewNavigator: RenameSessionViewNavigator

    private var renameEditTextInitialized = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSessionRenameBinding {
        return FragmentSessionRenameBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        initToolbar()
    }

    private fun initToolbar() {
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(R.string.device_manager_session_rename)
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is RenameSessionViewEvent.SessionRenamed -> {
                    viewNavigator.navigateBack(requireActivity())
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        if(renameEditTextInitialized.not()) {
            views.renameSessionEditText.setText(state.deviceName)
            renameEditTextInitialized = true
        }
    }
}
