/*
 * Copyright 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSettingsDevicesBinding
import javax.inject.Inject

/**
 * Display the list of the user's devices and sessions.
 */
@AndroidEntryPoint
class VectorSettingsDevicesFragment @Inject constructor() : VectorBaseFragment<FragmentSettingsDevicesBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsDevicesBinding {
        return FragmentSettingsDevicesBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initToolbar()
    }

    private fun initToolbar() {
        (activity as? AppCompatActivity)
                ?.supportActionBar
                ?.setTitle(R.string.settings_sessions_list)
    }
}
