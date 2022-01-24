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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.chip.Chip
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.core.utils.showIdentityServerConsentDialog
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.FragmentUserListBinding
import im.vector.app.features.homeserver.HomeServerCapabilitiesViewModel
import im.vector.app.features.settings.VectorSettingsActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

class UserListFragment @Inject constructor(
        private val userListController: UserListController,
        private val dimensionConverter: DimensionConverter,
) : VectorBaseFragment<FragmentUserListBinding>(),
        UserListController.Callback {

    private val args: UserListFragmentArgs by args()
    private val viewModel: UserListViewModel by activityViewModel()
    private val homeServerCapabilitiesViewModel: HomeServerCapabilitiesViewModel by fragmentViewModel()
    private lateinit var sharedActionViewModel: UserListSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUserListBinding {
        return FragmentUserListBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = args.menuResId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserListSharedActionViewModel::class.java)
        if (args.showToolbar) {
            setupToolbar(views.userListToolbar)
                    .setTitle(args.title)
                    .allowBack(useCross = true)
            views.userListToolbar.isVisible = true
        } else {
            views.userListToolbar.isVisible = false
        }
        setupRecyclerView()
        setupSearchView()

        homeServerCapabilitiesViewModel.onEach {
            views.userListE2EbyDefaultDisabled.isVisible = !it.isE2EByDefault
        }

        viewModel.onEach(UserListViewState::pendingSelections) {
            renderSelectedUsers(it)
        }

        viewModel.observeViewEvents {
            when (it) {
                is UserListViewEvents.OpenShareMatrixToLink -> {
                    val text = getString(R.string.invite_friends_text, it.link)
                    startSharePlainTextIntent(
                            fragment = this,
                            activityResultLauncher = null,
                            chooserTitle = getString(R.string.invite_friends),
                            text = text,
                            extraTitle = getString(R.string.invite_friends_rich_title)
                    )
                }
                is UserListViewEvents.Failure               -> showFailure(it.throwable)
                is UserListViewEvents.OnPoliciesRetrieved   -> showConsentDialog(it)
            }
        }
    }

    override fun onDestroyView() {
        views.userListRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) {
            val showMenuItem = it.pendingSelections.isNotEmpty()
            menu.forEach { menuItem ->
                menuItem.isVisible = showMenuItem
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = withState(viewModel) {
        sharedActionViewModel.post(UserListSharedAction.OnMenuItemSelected(item.itemId, it.pendingSelections))
        return@withState true
    }

    private fun setupRecyclerView() {
        userListController.callback = this
        // Don't activate animation as we might have way to much item animation when filtering
        views.userListRecyclerView.configureWith(userListController, disableItemAnimation = true)
    }

    private fun setupSearchView() {
        views.userListSearch
                .textChanges()
                .onStart { emit(views.userListSearch.text) }
                .onEach { text ->
                    val searchValue = text.trim()
                    val action = if (searchValue.isBlank()) {
                        UserListAction.ClearSearchUsers
                    } else {
                        UserListAction.SearchUsers(searchValue.toString())
                    }
                    viewModel.handle(action)
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.userListSearch.setupAsSearch()
        views.userListSearch.requestFocus()
    }

    override fun invalidate() = withState(viewModel) {
        userListController.setData(it)
    }

    private fun renderSelectedUsers(selections: Set<PendingSelection>) {
        invalidateOptionsMenu()

        val currentNumberOfChips = views.chipGroup.childCount
        val newNumberOfChips = selections.size

        views.chipGroup.removeAllViews()
        selections.forEach { addChipToGroup(it) }

        // Scroll to the bottom when adding chips. When removing chips, do not scroll
        if (newNumberOfChips >= currentNumberOfChips) {
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                views.chipGroupScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun addChipToGroup(pendingSelection: PendingSelection) {
        val chip = Chip(requireContext())
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = dimensionConverter.dpToPx(1).toFloat()
        chip.text = pendingSelection.getBestName()
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        views.chipGroup.addView(chip)
        chip.setOnCloseIconClickListener {
            viewModel.handle(UserListAction.RemovePendingSelection(pendingSelection))
        }
    }

    fun getCurrentState() = withState(viewModel) { it }

    override fun onInviteFriendClick() {
        viewModel.handle(UserListAction.ComputeMatrixToLinkForSharing)
    }

    override fun onContactBookClick() {
        sharedActionViewModel.post(UserListSharedAction.OpenPhoneBook)
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        viewModel.handle(UserListAction.AddPendingSelection(PendingSelection.UserPendingSelection(user)))
    }

    override fun onMatrixIdClick(matrixId: String) {
        view?.hideKeyboard()
        viewModel.handle(UserListAction.AddPendingSelection(PendingSelection.UserPendingSelection(User(matrixId))))
    }

    override fun onThreePidClick(threePid: ThreePid) {
        view?.hideKeyboard()
        viewModel.handle(UserListAction.AddPendingSelection(PendingSelection.ThreePidPendingSelection(threePid)))
    }

    override fun onSetupDiscovery() {
        navigator.openSettings(
                requireContext(),
                VectorSettingsActivity.EXTRA_DIRECT_ACCESS_DISCOVERY_SETTINGS
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.handle(UserListAction.Resumed)
    }

    override fun giveIdentityServerConsent() {
        viewModel.handle(UserListAction.UserConsentRequest)
    }

    private fun showConsentDialog(event: UserListViewEvents.OnPoliciesRetrieved) {
        requireContext().showIdentityServerConsentDialog(
                event.identityServerWithTerms,
                consentCallBack = { viewModel.handle(UserListAction.UpdateUserConsent(true)) }
        )
    }

    override fun onUseQRCode() {
        view?.hideKeyboard()
        sharedActionViewModel.post(UserListSharedAction.AddByQrCode)
    }
}
