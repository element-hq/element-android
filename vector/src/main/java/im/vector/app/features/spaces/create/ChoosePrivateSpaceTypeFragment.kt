/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentSpaceCreateChoosePrivateModelBinding
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class ChoosePrivateSpaceTypeFragment :
        VectorBaseFragment<FragmentSpaceCreateChoosePrivateModelBinding>(),
        OnBackPressed {

    @Inject lateinit var stringProvider: StringProvider

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
            views.accessInfoHelpText.text = stringProvider.getString(CommonStrings.create_spaces_make_sure_access, state.name ?: "")
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        sharedViewModel.handle(CreateSpaceAction.OnBackPressed)
        return true
    }
}
