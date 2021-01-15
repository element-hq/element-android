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

package im.vector.app.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentPromptSimplifiedModeBinding
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class PromptSimplifiedModeFragment @Inject constructor() : VectorBaseFragment<FragmentPromptSimplifiedModeBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPromptSimplifiedModeBinding {
        return FragmentPromptSimplifiedModeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.promptSimplifiedModeOn.setOnClickListener { simplifiedModeOn() }
        views.promptSimplifiedModeOff.setOnClickListener { simplifiedModeOff() }
    }

    private fun simplifiedModeOn() {
        VectorPreferences(requireContext()).setSimplifiedMode(true)
        activity?.finish()
    }

    private fun simplifiedModeOff() {
        VectorPreferences(requireContext()).setSimplifiedMode(false)
        activity?.finish()
    }
}
