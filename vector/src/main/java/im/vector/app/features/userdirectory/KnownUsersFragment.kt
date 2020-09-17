/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.userdirectory

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.chip.Chip
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.homeserver.HomeServerCapabilitiesViewModel
import kotlinx.android.synthetic.main.fragment_known_users.*
import org.matrix.android.sdk.api.session.user.model.User
import javax.inject.Inject

class KnownUsersFragment @Inject constructor(
        val userDirectoryViewModelFactory: UserDirectoryViewModel.Factory,
        private val knownUsersController: KnownUsersController,
        private val dimensionConverter: DimensionConverter,
        val homeServerCapabilitiesViewModelFactory: HomeServerCapabilitiesViewModel.Factory
) : VectorBaseFragment(), KnownUsersController.Callback {

    private val args: KnownUsersFragmentArgs by args()

    override fun getLayoutResId() = R.layout.fragment_known_users

    override fun getMenuRes() = args.menuResId

    private val viewModel: UserDirectoryViewModel by activityViewModel()
    private val homeServerCapabilitiesViewModel: HomeServerCapabilitiesViewModel by fragmentViewModel()

    private lateinit var sharedActionViewModel: UserDirectorySharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserDirectorySharedActionViewModel::class.java)

        knownUsersTitle.text = args.title
        vectorBaseActivity.setSupportActionBar(knownUsersToolbar)
        setupRecyclerView()
        setupFilterView()
        setupAddByMatrixIdView()
        setupAddFromPhoneBookView()
        setupCloseView()

        homeServerCapabilitiesViewModel.subscribe {
            knownUsersE2EbyDefaultDisabled.isVisible = !it.isE2EByDefault
        }

        viewModel.selectSubscribe(this, UserDirectoryViewState::pendingInvitees) {
            renderSelectedUsers(it)
        }
    }

    override fun onDestroyView() {
        knownUsersController.callback = null
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) {
            val showMenuItem = it.pendingInvitees.isNotEmpty()
            menu.forEach { menuItem ->
                menuItem.isVisible = showMenuItem
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = withState(viewModel) {
        sharedActionViewModel.post(UserDirectorySharedAction.OnMenuItemSelected(item.itemId, it.pendingInvitees))
        return@withState true
    }

    private fun setupAddByMatrixIdView() {
        addByMatrixId.debouncedClicks {
            sharedActionViewModel.post(UserDirectorySharedAction.OpenUsersDirectory)
        }
    }

    private fun setupAddFromPhoneBookView() {
        addFromPhoneBook.debouncedClicks {
            // TODO handle Permission first
            sharedActionViewModel.post(UserDirectorySharedAction.OpenPhoneBook)
        }
    }

    private fun setupRecyclerView() {
        knownUsersController.callback = this
        // Don't activate animation as we might have way to much item animation when filtering
        recyclerView.configureWith(knownUsersController, disableItemAnimation = true)
    }

    private fun setupFilterView() {
        knownUsersFilter
                .textChanges()
                .startWith(knownUsersFilter.text)
                .subscribe { text ->
                    val filterValue = text.trim()
                    val action = if (filterValue.isBlank()) {
                        UserDirectoryAction.ClearFilterKnownUsers
                    } else {
                        UserDirectoryAction.FilterKnownUsers(filterValue.toString())
                    }
                    viewModel.handle(action)
                }
                .disposeOnDestroyView()

        knownUsersFilter.setupAsSearch()
        knownUsersFilter.requestFocus()
    }

    private fun setupCloseView() {
        knownUsersClose.debouncedClicks {
            requireActivity().finish()
        }
    }

    override fun invalidate() = withState(viewModel) {
        knownUsersController.setData(it)
    }

    private fun renderSelectedUsers(invitees: Set<PendingInvitee>) {
        invalidateOptionsMenu()

        val currentNumberOfChips = chipGroup.childCount
        val newNumberOfChips = invitees.size

        chipGroup.removeAllViews()
        invitees.forEach { addChipToGroup(it) }

        // Scroll to the bottom when adding chips. When removing chips, do not scroll
        if (newNumberOfChips >= currentNumberOfChips) {
            chipGroupScrollView.post {
                chipGroupScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun addChipToGroup(pendingInvitee: PendingInvitee) {
        val chip = Chip(requireContext())
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = dimensionConverter.dpToPx(1).toFloat()
        chip.text = pendingInvitee.getBestName()
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chipGroup.addView(chip)
        chip.setOnCloseIconClickListener {
            viewModel.handle(UserDirectoryAction.RemovePendingInvitee(pendingInvitee))
        }
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(UserDirectoryAction.SelectPendingInvitee(PendingInvitee.UserPendingInvitee(user)))
    }
}
