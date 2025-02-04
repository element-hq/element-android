/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.explore

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.isValidUrl
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentSpaceDirectoryBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.spaces.manage.ManageType
import im.vector.app.features.spaces.manage.SpaceAddRoomSpaceChooserBottomSheet
import im.vector.app.features.spaces.manage.SpaceManageActivity
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import java.net.URL
import javax.inject.Inject

@Parcelize
data class SpaceDirectoryArgs(
        val spaceId: String
) : Parcelable

@AndroidEntryPoint
class SpaceDirectoryFragment :
        VectorBaseFragment<FragmentSpaceDirectoryBinding>(),
        SpaceDirectoryController.InteractionListener,
        TimelineEventController.UrlClickCallback,
        OnBackPressed,
        VectorMenuProvider {

    @Inject lateinit var epoxyController: SpaceDirectoryController
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var colorProvider: ColorProvider

    override fun getMenuRes() = R.menu.menu_space_directory

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceDirectoryBinding.inflate(layoutInflater, container, false)

    private val viewModel by activityViewModel(SpaceDirectoryViewModel::class)
    private val epoxyVisibilityTracker = EpoxyVisibilityTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SpaceExploreRooms
        childFragmentManager.setFragmentResultListener(SpaceAddRoomSpaceChooserBottomSheet.REQUEST_KEY, this) { _, bundle ->

            bundle.getString(SpaceAddRoomSpaceChooserBottomSheet.BUNDLE_KEY_ACTION)?.let { action ->
                val spaceId = withState(viewModel) { it.spaceId }
                when (action) {
                    SpaceAddRoomSpaceChooserBottomSheet.ACTION_ADD_ROOMS -> {
                        addExistingRoomActivityResult.launch(SpaceManageActivity.newIntent(requireContext(), spaceId, ManageType.AddRooms))
                    }
                    SpaceAddRoomSpaceChooserBottomSheet.ACTION_ADD_SPACES -> {
                        addExistingRoomActivityResult.launch(SpaceManageActivity.newIntent(requireContext(), spaceId, ManageType.AddRoomsOnlySpaces))
                    }
                    SpaceAddRoomSpaceChooserBottomSheet.ACTION_CREATE_ROOM -> {
                        viewModel.handle(SpaceDirectoryViewAction.CreateNewRoom)
                    }
                    else -> {
                        // nop
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.toolbar)
                .allowBack()

        epoxyController.listener = this
        views.spaceDirectoryList.configureWith(epoxyController)
        epoxyVisibilityTracker.attach(views.spaceDirectoryList)

        viewModel.onEach(SpaceDirectoryState::canAddRooms) {
            invalidateOptionsMenu()
        }

        views.addOrCreateChatRoomButton.debouncedClicks {
            withState(viewModel) {
                addExistingRooms(it.spaceId)
            }
        }

        // Hide FAB when list is scrolling
        views.spaceDirectoryList.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        views.addOrCreateChatRoomButton.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                views.addOrCreateChatRoomButton.postDelayed(showFabRunnable, 250)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                views.addOrCreateChatRoomButton.hide()
                            }
                        }
                    }
                })
    }

    override fun onDestroyView() {
        epoxyController.listener = null
        epoxyVisibilityTracker.detach(views.spaceDirectoryList)
        views.spaceDirectoryList.cleanup()
        super.onDestroyView()
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            views.addOrCreateChatRoomButton.show()
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)

        val currentParentId = state.hierarchyStack.lastOrNull()

        if (currentParentId == null) {
            // it's the root
            toolbar?.setTitle(CommonStrings.space_explore_activity_title)
        } else {
            val spaceName = state.currentRootSummary?.name
                    ?: state.currentRootSummary?.canonicalAlias

            if (spaceName != null) {
                toolbar?.title = spaceName
                toolbar?.subtitle = getString(CommonStrings.space_explore_activity_title)
            } else {
                toolbar?.title = getString(CommonStrings.space_explore_activity_title)
            }
        }

        views.addOrCreateChatRoomButton.isVisible = state.canAddRooms
    }

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            menu.findItem(R.id.spaceAddRoom)?.isVisible = state.canAddRooms
            menu.findItem(R.id.spaceCreateRoom)?.isVisible = false // Not yet implemented

            menu.findItem(R.id.spaceSearch)?.let { searchItem ->
                val searchView = searchItem.actionView as SearchView
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        onFilterQueryChanged(newText)
                        return true
                    }
                })
            }
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.spaceAddRoom -> {
                withState(viewModel) { state ->
                    addExistingRooms(state.spaceId)
                }
                true
            }
            R.id.spaceCreateRoom -> {
                // not implemented yet
                true
            }
            else -> false
        }
    }

    override fun onFilterQueryChanged(query: String?) {
        viewModel.handle(SpaceDirectoryViewAction.FilterRooms(query))
    }

    override fun onButtonClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.JoinOrOpen(spaceChildInfo))
    }

    override fun onSpaceChildClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.ExploreSubSpace(spaceChildInfo))
    }

    override fun onRoomClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.ShowDetails(spaceChildInfo))
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        viewModel.handle(SpaceDirectoryViewAction.HandleBack)
        return true
    }

    override fun retry() {
        viewModel.handle(SpaceDirectoryViewAction.Retry)
    }

    private val addExistingRoomActivityResult = registerStartForActivityResult { _ ->
        viewModel.handle(SpaceDirectoryViewAction.Retry)
    }

    override fun addExistingRooms(spaceId: String) {
        SpaceAddRoomSpaceChooserBottomSheet.newInstance().show(childFragmentManager, "SpaceAddRoomSpaceChooserBottomSheet")
    }

    override fun loadAdditionalItemsIfNeeded() {
        viewModel.handle(SpaceDirectoryViewAction.LoadAdditionalItemsIfNeeded)
    }

    override fun onUrlClicked(url: String, title: String): Boolean {
        viewLifecycleOwner.lifecycleScope.launch {
            val isHandled = permalinkHandler.launch(requireActivity(), url, null)
            if (!isHandled) {
                if (title.isValidUrl() && url.isValidUrl() && URL(title).host != URL(url).host) {
                    MaterialAlertDialogBuilder(requireActivity(), im.vector.lib.ui.styles.R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                            .setTitle(CommonStrings.external_link_confirmation_title)
                            .setMessage(
                                    getString(CommonStrings.external_link_confirmation_message, title, url)
                                            .toSpannable()
                                            .colorizeMatchingText(
                                                    url,
                                                    colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_tertiary)
                                            )
                                            .colorizeMatchingText(
                                                    title,
                                                    colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_tertiary)
                                            )
                            )
                            .setPositiveButton(CommonStrings._continue) { _, _ ->
                                openUrlInExternalBrowser(requireContext(), url)
                            }
                            .setNegativeButton(CommonStrings.action_cancel, null)
                            .show()
                } else {
                    // Open in external browser, in a new Tab
                    openUrlInExternalBrowser(requireContext(), url)
                }
            }
        }
        // In fact it is always managed
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        // nothing?
        return false
    }
}
