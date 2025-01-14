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
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceCreateGenericEpoxyFormBinding
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class CreateSpaceDefaultRoomsFragment :
        VectorBaseFragment<FragmentSpaceCreateGenericEpoxyFormBinding>(),
        SpaceDefaultRoomEpoxyController.Listener,
        OnBackPressed {

    @Inject lateinit var epoxyController: SpaceDefaultRoomEpoxyController

    private val sharedViewModel: CreateSpaceViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceCreateGenericEpoxyFormBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.recyclerView.configureWith(epoxyController)
        epoxyController.listener = this

        sharedViewModel.onEach {
            epoxyController.setData(it)
        }

        views.nextButton.setText(CommonStrings.create_space)
        views.nextButton.debouncedClicks {
            view.hideKeyboard()
            sharedViewModel.handle(CreateSpaceAction.NextFromDefaultRooms)
        }
    }

    override fun onNameChange(index: Int, newName: String) {
        sharedViewModel.handle(CreateSpaceAction.DefaultRoomNameChanged(index, newName))
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        sharedViewModel.handle(CreateSpaceAction.OnBackPressed)
        return true
    }
}
