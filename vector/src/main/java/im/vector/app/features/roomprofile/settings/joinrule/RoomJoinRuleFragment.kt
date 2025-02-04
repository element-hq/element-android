/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentJoinRulesRecyclerBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedActions
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedViewModel
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

@AndroidEntryPoint
class RoomJoinRuleFragment :
        VectorBaseFragment<FragmentJoinRulesRecyclerBinding>(),
        OnBackPressed,
        RoomJoinRuleAdvancedController.InteractionListener {

    @Inject lateinit var controller: RoomJoinRuleAdvancedController
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private val viewModel: RoomJoinRuleChooseRestrictedViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentJoinRulesRecyclerBinding.inflate(inflater, container, false)

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        val hasUnsavedChanges = withState(viewModel) { it.hasUnsavedChanges }
        val isLoading = withState(viewModel) { it.updatingStatus is Loading }
        if (!hasUnsavedChanges || isLoading) {
            requireActivity().finish()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(CommonStrings.dialog_title_warning)
                    .setMessage(CommonStrings.warning_unsaved_change)
                    .setPositiveButton(CommonStrings.warning_unsaved_change_discard) { _, _ ->
                        requireActivity().finish()
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
            return true
        }
        return true
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        controller.setData(state)
        if (state.hasUnsavedChanges) {
            // show discard and save
            views.cancelButton.isVisible = true
            views.positiveButton.text = getString(CommonStrings.warning_unsaved_change_discard)
            views.positiveButton.isVisible = true
            views.positiveButton.text = getString(CommonStrings.action_save)
            views.positiveButton.debouncedClicks {
                viewModel.handle(RoomJoinRuleChooseRestrictedActions.DoUpdateJoinRules)
            }
        } else {
            views.cancelButton.isVisible = false
            views.positiveButton.isVisible = true
            views.positiveButton.text = getString(CommonStrings.ok)
            views.positiveButton.debouncedClicks { requireActivity().finish() }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(controller, hasFixedSize = true)
        controller.interactionListener = this
        views.cancelButton.debouncedClicks { requireActivity().finish() }
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun didSelectRule(rules: RoomJoinRules) {
        val isLoading = withState(viewModel) { it.updatingStatus is Loading }
        if (isLoading) return

        viewModel.handle(RoomJoinRuleChooseRestrictedActions.SelectJoinRules(rules))
    }
}
