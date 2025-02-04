/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.leave

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.ToggleableAppBarLayoutBehavior
import im.vector.app.databinding.FragmentSpaceLeaveAdvancedBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

@AndroidEntryPoint
class SpaceLeaveAdvancedFragment :
        VectorBaseFragment<FragmentSpaceLeaveAdvancedBinding>(),
        SelectChildrenController.Listener,
        VectorMenuProvider {

    @Inject lateinit var controller: SelectChildrenController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceLeaveAdvancedBinding.inflate(layoutInflater, container, false)

    val viewModel: SpaceLeaveAdvancedViewModel by activityViewModel()

    override fun getMenuRes() = R.menu.menu_space_leave

    override fun handleMenuItemSelected(item: MenuItem) = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.listener = this

        withState(viewModel) { state ->
            setupToolbar(views.toolbar)
                    .setSubtitle(state.spaceSummary?.name)
                    .allowBack()

            state.spaceSummary?.let { summary ->
                val warningMessage: CharSequence? = when {
                    summary.otherMemberIds.isEmpty() -> getString(CommonStrings.space_leave_prompt_msg_only_you)
                    state.isLastAdmin -> getString(CommonStrings.space_leave_prompt_msg_as_admin)
                    !summary.isPublic -> getString(CommonStrings.space_leave_prompt_msg_private)
                    else -> null
                }

                views.spaceLeavePromptDescription.isVisible = warningMessage != null
                views.spaceLeavePromptDescription.text = warningMessage
            }

            views.spaceLeavePromptTitle.text = getString(CommonStrings.space_leave_prompt_msg_with_name, state.spaceSummary?.name ?: "")
        }

        views.roomList.configureWith(controller)
        views.spaceLeaveCancel.debouncedClicks { requireActivity().finish() }

        views.spaceLeaveButton.debouncedClicks {
            viewModel.handle(SpaceLeaveAdvanceViewAction.DoLeave)
        }

        views.spaceLeaveSelectGroup.setOnCheckedChangeListener { _, optionId ->
            when (optionId) {
                R.id.spaceLeaveSelectAll -> viewModel.handle(SpaceLeaveAdvanceViewAction.SelectAll)
                R.id.spaceLeaveSelectNone -> viewModel.handle(SpaceLeaveAdvanceViewAction.SelectNone)
            }
        }
    }

    override fun handlePrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_space_leave_search)?.let { searchItem ->
            searchItem.bind(
                    onExpanded = { viewModel.handle(SpaceLeaveAdvanceViewAction.SetFilteringEnabled(isEnabled = true)) },
                    onCollapsed = { viewModel.handle(SpaceLeaveAdvanceViewAction.SetFilteringEnabled(isEnabled = false)) },
                    onTextChanged = { viewModel.handle(SpaceLeaveAdvanceViewAction.UpdateFilter(it)) }
            )
        }
    }

    override fun onDestroyView() {
        controller.listener = null
        views.roomList.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()

        if (state.isFilteringEnabled) {
            views.appBarLayout.setExpanded(false)
        }

        updateAppBarBehaviorState(state)
        updateRadioButtonsState(state)

        controller.setData(state)
    }

    override fun onItemSelected(roomSummary: RoomSummary) {
        viewModel.handle(SpaceLeaveAdvanceViewAction.ToggleSelection(roomSummary.roomId))
    }

    private fun updateAppBarBehaviorState(state: SpaceLeaveAdvanceViewState) {
        val behavior = (views.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as ToggleableAppBarLayoutBehavior
        behavior.isEnabled = !state.isFilteringEnabled
    }

    private fun updateRadioButtonsState(state: SpaceLeaveAdvanceViewState) {
        (state.allChildren as? Success)?.invoke()?.size?.let { allChildrenCount ->
            when (state.selectedRooms.size) {
                0 -> views.spaceLeaveSelectNone.isChecked = true
                allChildrenCount -> views.spaceLeaveSelectAll.isChecked = true
                else -> views.spaceLeaveSelectSemi.isChecked = true
            }
        }
    }

    private fun MenuItem.bind(
            onExpanded: () -> Unit,
            onCollapsed: () -> Unit,
            onTextChanged: (String) -> Unit
    ) {
        setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                onExpanded()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                onCollapsed()
                return true
            }
        })

        val searchView = actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onTextChanged(newText ?: "")
                return true
            }
        })
    }
}
