/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.platform.showOptimizedSnackbar
import im.vector.app.core.utils.toast
import im.vector.app.databinding.FragmentPublicRoomsBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkFactory
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import reactivecircus.flowbinding.appcompat.queryTextChanges
import timber.log.Timber
import javax.inject.Inject

/**
 * What can be improved:
 * - When filtering more (when entering new chars), we could filter on result we already have, during the new server request, to avoid empty screen effect.
 */
@AndroidEntryPoint
class PublicRoomsFragment :
        VectorBaseFragment<FragmentPublicRoomsBinding>(),
        PublicRoomsController.Callback,
        VectorMenuProvider {

    @Inject lateinit var publicRoomsController: PublicRoomsController
    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var permalinkFactory: PermalinkFactory

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPublicRoomsBinding {
        return FragmentPublicRoomsBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.menu_room_directory

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.publicRoomsToolbar)
                .allowBack()

        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()

        views.publicRoomsFilter.queryTextChanges()
                .debounce(500)
                .onEach {
                    viewModel.handle(RoomDirectoryAction.FilterWith(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.publicRoomsCreateNewRoom.debouncedClicks {
            sharedActionViewModel.post(RoomDirectorySharedAction.CreateRoom)
        }

        viewModel.observeViewEvents {
            handleViewEvents(it)
        }
    }

    private fun handleViewEvents(viewEvents: RoomDirectoryViewEvents) {
        when (viewEvents) {
            is RoomDirectoryViewEvents.Failure -> {
                views.coordinatorLayout.showOptimizedSnackbar(errorFormatter.toHumanReadable(viewEvents.throwable))
            }
        }
    }

    override fun onDestroyView() {
        publicRoomsController.callback = null
        views.publicRoomsList.cleanup()
        super.onDestroyView()
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_room_directory_change_protocol -> {
                sharedActionViewModel.post(RoomDirectorySharedAction.ChangeProtocol)
                true
            }
            else -> false
        }
    }

    private fun setupRecyclerView() {
        views.publicRoomsList.trackItemsVisibilityChange()
        views.publicRoomsList.configureWith(publicRoomsController)
        publicRoomsController.callback = this
    }

    override fun onUnknownRoomClicked(roomIdOrAlias: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val permalink = permalinkFactory.createPermalink(roomIdOrAlias)
            val isHandled = permalinkHandler
                    .launch(requireActivity(), permalink, object : NavigationInterceptor {
                        override fun navToRoom(roomId: String?, eventId: String?, deepLink: Uri?, rootThreadEventId: String?): Boolean {
                            requireActivity().finish()
                            return false
                        }
                    })

            if (!isHandled) {
                requireContext().toast(CommonStrings.room_error_not_found)
            }
        }
    }

    override fun onPublicRoomClicked(publicRoom: PublicRoom, joinState: JoinState) {
        Timber.v("PublicRoomClicked: $publicRoom")
        withState(viewModel) { state ->
            when (joinState) {
                JoinState.JOINED -> {
                    navigator.openRoom(
                            context = requireActivity(),
                            roomId = publicRoom.roomId,
                            trigger = ViewRoom.Trigger.RoomDirectory
                    )
                }
                else -> {
                    // ROOM PREVIEW
                    navigator.openRoomPreview(requireActivity(), publicRoom, state.roomDirectoryData)
                }
            }
        }
    }

    override fun onPublicRoomJoin(publicRoom: PublicRoom) {
        Timber.v("PublicRoomJoinClicked: $publicRoom")
        viewModel.handle(RoomDirectoryAction.JoinRoom(publicRoom))
    }

    override fun loadMore() {
        viewModel.handle(RoomDirectoryAction.LoadMore)
    }

    private var initialValueSet = false

    override fun invalidate() = withState(viewModel) { state ->
        if (!initialValueSet) {
            initialValueSet = true
            if (views.publicRoomsFilter.query.toString() != state.currentFilter) {
                // For initial filter
                views.publicRoomsFilter.setQuery(state.currentFilter, false)
            }
        }

        // Populate list with Epoxy
        publicRoomsController.setData(state)
    }
}
