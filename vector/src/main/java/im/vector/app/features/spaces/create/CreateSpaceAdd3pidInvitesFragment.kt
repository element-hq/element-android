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
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceCreateGenericEpoxyFormBinding
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class CreateSpaceAdd3pidInvitesFragment :
        VectorBaseFragment<FragmentSpaceCreateGenericEpoxyFormBinding>(),
        SpaceAdd3pidEpoxyController.Listener,
        OnBackPressed {

    @Inject lateinit var epoxyController: SpaceAdd3pidEpoxyController

    private val sharedViewModel: CreateSpaceViewModel by activityViewModel()

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        sharedViewModel.handle(CreateSpaceAction.OnBackPressed)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.recyclerView.configureWith(epoxyController)
        epoxyController.listener = this

        sharedViewModel.onEach {
            invalidateState(it)
        }

        views.nextButton.setText(CommonStrings.action_next)
        views.nextButton.debouncedClicks {
            view.hideKeyboard()
            sharedViewModel.handle(CreateSpaceAction.NextFromAdd3pid)
        }
    }

    private fun invalidateState(it: CreateSpaceState) {
        epoxyController.setData(it)
        val noEmails = it.default3pidInvite?.all { it.value.isNullOrBlank() } ?: true
        views.nextButton.text = if (noEmails) {
            getString(CommonStrings.skip_for_now)
        } else {
            getString(CommonStrings.action_next)
        }
    }

    override fun onDestroyView() {
        views.recyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceCreateGenericEpoxyFormBinding.inflate(layoutInflater, container, false)

    override fun on3pidChange(index: Int, newName: String) {
        sharedViewModel.handle(CreateSpaceAction.DefaultInvite3pidChanged(index, newName))
    }

    override fun onNoIdentityServer() {
        navigator.openSettings(
                requireContext(),
                VectorSettingsActivity.EXTRA_DIRECT_ACCESS_DISCOVERY_SETTINGS
        )
    }
}
