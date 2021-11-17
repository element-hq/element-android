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

package im.vector.app.features.spaces.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentSpaceCreateChoosePrivateModelBinding
import javax.inject.Inject

class ChoosePrivateSpaceTypeFragment @Inject constructor(
        private val stringProvider: StringProvider
) : VectorBaseFragment<FragmentSpaceCreateChoosePrivateModelBinding>(), OnBackPressed {

    private val sharedViewModel: CreateSpaceViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceCreateChoosePrivateModelBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.justMeButton.onClick {
            sharedViewModel.handle(CreateSpaceAction.SetSpaceTopology(SpaceTopology.JustMe))
        }

        views.teammatesButton.onClick {
            sharedViewModel.handle(CreateSpaceAction.SetSpaceTopology(SpaceTopology.MeAndTeammates))
        }

        sharedViewModel.onEach { state ->
            views.accessInfoHelpText.text = stringProvider.getString(R.string.create_spaces_make_sure_access, state.name ?: "")
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        sharedViewModel.handle(CreateSpaceAction.OnBackPressed)
        return true
    }
}
