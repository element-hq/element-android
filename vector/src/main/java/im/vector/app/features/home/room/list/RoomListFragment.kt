/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.aemerse.slider.ImageCarousel
import com.aemerse.slider.listener.CarouselListener
import com.aemerse.slider.model.CarouselItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.facebook.react.bridge.UiThreadUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.databinding.FragmentRoomListBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.discovery.DiscoverySettingsAction
import im.vector.app.features.discovery.DiscoverySettingsViewModel
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.filtered.FilteredRoomFooterItem
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.home.room.list.widget.NotifsFabMenuView
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.notifications.NotificationDrawerManager
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@Parcelize
data class RoomListParams(
        val displayMode: RoomListDisplayMode
) : Parcelable

@AndroidEntryPoint
class RoomListFragment :
        VectorBaseFragment<FragmentRoomListBinding>(),
        RoomListListener,
        OnBackPressed,
        FilteredRoomFooterItem.Listener,
        NotifsFabMenuView.Listener {

    @Inject lateinit var pagedControllerFactory: RoomSummaryPagedControllerFactory
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var footerController: RoomListFooterController
    @Inject lateinit var userPreferencesProvider: UserPreferencesProvider
    @Inject lateinit var stringProvider: StringProvider

    private var modelBuildListener: OnModelBuildFinishedListener? = null
    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private val roomListParams: RoomListParams by args()
    private val roomListViewModel: RoomListViewModel by fragmentViewModel()
    private val discoveryViewModel by fragmentViewModel(DiscoverySettingsViewModel::class)
    private lateinit var stateRestorer: LayoutManagerStateRestorer
    private var rootUrl: String = ""

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomListBinding {
        return FragmentRoomListBinding.inflate(inflater, container, false)
    }

    data class SectionKey(
            val name: String,
            val isExpanded: Boolean,
            val notifyOfLocalEcho: Boolean
    )

    data class SectionAdapterInfo(
            var section: SectionKey,
            val sectionHeaderAdapter: SectionHeaderAdapter,
            val contentEpoxyController: EpoxyController
    )

    private val adapterInfosList = mutableListOf<SectionAdapterInfo>()
    private var concatAdapter: ConcatAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = when (roomListParams.displayMode) {
            RoomListDisplayMode.PEOPLE -> MobileScreen.ScreenName.People
            RoomListDisplayMode.ROOMS -> MobileScreen.ScreenName.Rooms
            else -> null
        }

        discoveryViewModel.handle(DiscoverySettingsAction.ChangeIdentityServer(stringProvider.getString(R.string.matrix_org_server_url)))
        rootUrl = stringProvider.getString(R.string.backend_server_url)

//        TODO: Forced logout
//        if (!requireActivity().getSharedPreferences("bigstar", AppCompatActivity.MODE_PRIVATE).getBoolean(
//                        "isLoggedIn",
//                        false
//                )) {
//            SignOutUiWorker(requireActivity()).doSignOut()
//        }
    }

//    TODO: Country activity
//    private fun citiesSelection() {
//        if (requireActivity().getSharedPreferences("bigstar", AppCompatActivity.MODE_PRIVATE).getString(
//                        "city",
//                        ""
//                )!!.isEmpty()) {
//            startActivity(Intent(activity, CountriesActivity::class.java))
//        } else {
//            setupAdBanners()
//        }
//    }

    private fun setupAdBanners() {
        val carousel: ImageCarousel = views.carousel
        var ads = JSONArray();

        carousel.registerLifecycle(lifecycle)
        carousel.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
//                TODO: AdPopUp modal
//                AdPopUp(
//                        requireActivity(),
//                        requireContext(),
//                        ads.getJSONObject(position).getString("title"),
//                        ads.getJSONObject(position).getString("description"),
//                        "$rootUrl/files/" + ads.getJSONObject(position).getString("bannerUuid"),
//                        ads.getJSONObject(position)
//                ).show()

                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .patch(FormBody.Builder().build())
                        .url(rootUrl + "/ads/" + ads.getJSONObject(position).getString("uuid") + "/click")
                        .build()
                okHttpClient.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                })
            }
        }

        val list = mutableListOf<CarouselItem>()

        Thread {

            val url =
                    URL(
                            "$rootUrl/ads/client?cityUuid=" + requireActivity().getSharedPreferences("bigstar", AppCompatActivity.MODE_PRIVATE).getString(
                                    "city",
                                    ""
                            )!!
                    )
            if (isOnline(requireContext())) {
                with(url.openConnection() as HttpURLConnection) {
                    inputStream.bufferedReader().use {
                        it.readLines().forEach { line ->
                            ads = JSONArray(line);
                            for (i in 0 until ads.length()) {
                                list.add(
                                        CarouselItem(
                                                imageUrl = "$rootUrl/files/" + ads.getJSONObject(i)
                                                        .getString("thumbnailUuid")
                                        )
                                )
                                ads.getJSONObject(i)
                            }
                        }
                    }
                    UiThreadUtil.runOnUiThread {
                        carousel.setData(list)
                    }
                }
            }
        }.start()
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Timber.tag("Internet").i("NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Timber.tag("Internet").i("NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Timber.tag("Internet").i("NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.stateView.contentView = views.roomListView
        views.stateView.state = StateView.State.Loading
        setupCreateRoomButton()
        setupRecyclerView()
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        roomListViewModel.observeViewEvents {
            when (it) {
                is RoomListViewEvents.Loading -> showLoading(it.message)
                is RoomListViewEvents.Failure -> showFailure(it.throwable)
                is RoomListViewEvents.SelectRoom -> handleSelectRoom(it, it.isInviteAlreadyAccepted)
                is RoomListViewEvents.Done -> Unit
                is RoomListViewEvents.NavigateToMxToBottomSheet -> handleShowMxToLink(it.link)
            }
        }

        views.createChatFabMenu.listener = this

        sharedActionViewModel
                .stream()
                .onEach { handleQuickActions(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        roomListViewModel.onEach(RoomListViewState::roomMembershipChanges) { ms ->
            // it's for invites local echo
            adapterInfosList.filter { it.section.notifyOfLocalEcho }
                    .onEach {
                        (it.contentEpoxyController as? RoomSummaryPagedController)?.roomChangeMembershipStates = ms
                    }
        }
    }

    override fun onStart() {
        super.onStart()

        // Local rooms should not exist anymore when the room list is shown
        roomListViewModel.handle(RoomListAction.DeleteAllLocalRoom)
    }

    private fun refreshCollapseStates() {
        val sectionsCount = adapterInfosList.count { !it.sectionHeaderAdapter.roomsSectionData.isHidden }
        roomListViewModel.sections.forEachIndexed { index, roomsSection ->
            val actualBlock = adapterInfosList[index]
            val isRoomSectionCollapsable = sectionsCount > 1
            val isRoomSectionExpanded = roomsSection.isExpanded.value.orTrue()
            if (actualBlock.section.isExpanded && !isRoomSectionExpanded) {
                // mark controller as collapsed
                actualBlock.contentEpoxyController.setCollapsed(true)
            } else if (!actualBlock.section.isExpanded && isRoomSectionExpanded) {
                // we must expand!
                actualBlock.contentEpoxyController.setCollapsed(false)
            }
            actualBlock.section = actualBlock.section.copy(isExpanded = isRoomSectionExpanded)
            actualBlock.sectionHeaderAdapter.updateSection {
                it.copy(
                        isExpanded = isRoomSectionExpanded,
                        isCollapsable = isRoomSectionCollapsable
                )
            }

            if (!isRoomSectionExpanded && !isRoomSectionCollapsable) {
                // force expand if the section is not collapsable
                roomListViewModel.handle(RoomListAction.ToggleSection(roomsSection))
            }
        }
    }

    override fun showFailure(throwable: Throwable) {
        showErrorInSnackbar(throwable)
    }

    private fun handleShowMxToLink(link: String) {
        navigator.openMatrixToBottomSheet(requireActivity(), link, OriginOfMatrixTo.ROOM_LIST)
    }

    override fun onDestroyView() {
        adapterInfosList.onEach { it.contentEpoxyController.removeModelBuildListener(modelBuildListener) }
        adapterInfosList.clear()
        modelBuildListener = null
        views.roomListView.cleanup()
        footerController.listener = null
        // TODO Cleanup listener on the ConcatAdapter's adapters?
        stateRestorer.clear()
        views.createChatFabMenu.listener = null
        concatAdapter = null
        super.onDestroyView()
    }

    private fun handleSelectRoom(event: RoomListViewEvents.SelectRoom, isInviteAlreadyAccepted: Boolean) {
        navigator.openRoom(
                context = requireActivity(),
                roomId = event.roomSummary.roomId,
                isInviteAlreadyAccepted = isInviteAlreadyAccepted,
                trigger = ViewRoom.Trigger.RoomList
        )
    }

    private fun setupCreateRoomButton() {
        when (roomListParams.displayMode) {
            RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.isVisible = true
            RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.isVisible = true
            RoomListDisplayMode.ROOMS -> views.createGroupRoomButton.isVisible = true
            RoomListDisplayMode.FILTERED -> Unit // No button in this mode
        }

        views.createChatRoomButton.debouncedClicks {
            fabCreateDirectChat()
        }
        views.createGroupRoomButton.debouncedClicks {
            fabOpenRoomDirectory()
        }

        // Hide FAB when list is scrolling
        views.roomListView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        views.createChatFabMenu.removeCallbacks(showFabRunnable)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                views.createChatFabMenu.postDelayed(showFabRunnable, 250)
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                when (roomListParams.displayMode) {
                                    RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.hide()
                                    RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.hide()
                                    RoomListDisplayMode.ROOMS -> views.createGroupRoomButton.hide()
                                    RoomListDisplayMode.FILTERED -> Unit
                                }
                            }
                        }
                    }
                })
    }

    fun filterRoomsWith(filter: String) {
        // Scroll the list to top
        views.roomListView.scrollToPosition(0)

        roomListViewModel.handle(RoomListAction.FilterWith(filter))
    }

    // FilteredRoomFooterItem.Listener
    override fun createRoom(initialName: String) {
        navigator.openCreateRoom(requireActivity(), initialName)
    }

    override fun createDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    override fun openRoomDirectory(initialFilter: String) {
        navigator.openRoomDirectory(requireActivity(), initialFilter)
    }

    // NotifsFabMenuView.Listener
    override fun fabCreateDirectChat() {
        navigator.openCreateDirectRoom(requireActivity())
    }

    override fun fabOpenRoomDirectory() {
        navigator.openRoomDirectory(requireActivity(), "")
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        views.roomListView.layoutManager = layoutManager
        views.roomListView.itemAnimator = RoomListAnimator()
        layoutManager.recycleChildrenOnDetach = true

        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }

        val concatAdapter = ConcatAdapter()

        roomListViewModel.sections.forEachIndexed { index, section ->
            val sectionAdapter = SectionHeaderAdapter(SectionHeaderAdapter.RoomsSectionData(section.sectionName)) {
                if (adapterInfosList[index].sectionHeaderAdapter.roomsSectionData.isCollapsable) {
                    roomListViewModel.handle(RoomListAction.ToggleSection(section))
                }
            }
            val contentAdapter =
                    when {
                        section.livePages != null -> {
                            pagedControllerFactory.createRoomSummaryPagedController(roomListParams.displayMode)
                                    .also { controller ->
                                        section.livePages.observe(viewLifecycleOwner) { pl ->
                                            controller.submitList(pl)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = pl.isEmpty(),
                                                        isLoading = false
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        notificationCount = counts.totalCount,
                                                        isHighlighted = counts.isHighlight,
                                                )
                                            }
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        section.liveSuggested != null -> {
                            pagedControllerFactory.createSuggestedRoomListController()
                                    .also { controller ->
                                        section.liveSuggested.observe(viewLifecycleOwner) { info ->
                                            controller.setData(info)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = info.rooms.isEmpty(),
                                                        isLoading = false
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                        else -> {
                            pagedControllerFactory.createRoomSummaryListController(roomListParams.displayMode)
                                    .also { controller ->
                                        section.liveList?.observe(viewLifecycleOwner) { list ->
                                            controller.setData(list)
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        isHidden = list.isEmpty(),
                                                        isLoading = false,
                                                )
                                            }
                                            refreshCollapseStates()
                                            checkEmptyState()
                                        }
                                        observeItemCount(section, sectionAdapter)
                                        section.notificationCount.observe(viewLifecycleOwner) { counts ->
                                            sectionAdapter.updateSection {
                                                it.copy(
                                                        notificationCount = counts.totalCount,
                                                        isHighlighted = counts.isHighlight
                                                )
                                            }
                                        }
                                        section.isExpanded.observe(viewLifecycleOwner) {
                                            refreshCollapseStates()
                                        }
                                        controller.listener = this
                                    }
                        }
                    }
            adapterInfosList.add(
                    SectionAdapterInfo(
                            SectionKey(
                                    name = section.sectionName,
                                    isExpanded = section.isExpanded.value.orTrue(),
                                    notifyOfLocalEcho = section.notifyOfLocalEcho
                            ),
                            sectionAdapter,
                            contentAdapter
                    )
            )
            concatAdapter.addAdapter(sectionAdapter)
            concatAdapter.addAdapter(contentAdapter.adapter)
        }

        // Add the footer controller
        footerController.listener = this
        concatAdapter.addAdapter(footerController.adapter)

        this.concatAdapter = concatAdapter
        views.roomListView.adapter = concatAdapter
    }

    private val showFabRunnable = Runnable {
        if (isAdded) {
            when (roomListParams.displayMode) {
                RoomListDisplayMode.NOTIFICATIONS -> views.createChatFabMenu.show()
                RoomListDisplayMode.PEOPLE -> views.createChatRoomButton.show()
                RoomListDisplayMode.ROOMS -> views.createGroupRoomButton.show()
                RoomListDisplayMode.FILTERED -> Unit
            }
        }
    }

    private fun observeItemCount(section: RoomsSection, sectionAdapter: SectionHeaderAdapter) {
        lifecycleScope.launch {
            section.itemCount
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .filter { it > 0 }
                    .collect { count ->
                        sectionAdapter.updateSection {
                            it.copy(itemCount = count)
                        }
                    }
        }
    }

    private fun handleQuickActions(quickAction: RoomListQuickActionsSharedAction) {
        when (quickAction) {
            is RoomListQuickActionsSharedAction.NotificationsAllNoisy -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES_NOISY))
            }
            is RoomListQuickActionsSharedAction.NotificationsAll -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.ALL_MESSAGES))
            }
            is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MENTIONS_ONLY))
            }
            is RoomListQuickActionsSharedAction.NotificationsMute -> {
                roomListViewModel.handle(RoomListAction.ChangeRoomNotificationState(quickAction.roomId, RoomNotificationState.MUTE))
            }
            is RoomListQuickActionsSharedAction.Settings -> {
                navigator.openRoomProfile(requireActivity(), quickAction.roomId)
            }
            is RoomListQuickActionsSharedAction.Favorite -> {
                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_FAVOURITE))
            }
            is RoomListQuickActionsSharedAction.LowPriority -> {
                roomListViewModel.handle(RoomListAction.ToggleTag(quickAction.roomId, RoomTag.ROOM_TAG_LOW_PRIORITY))
            }
            is RoomListQuickActionsSharedAction.Leave -> {
                promptLeaveRoom(quickAction.roomId)
            }
        }
    }

    private fun promptLeaveRoom(roomId: String) {
        val isPublicRoom = roomListViewModel.isPublicRoom(roomId)
        val message = buildString {
            append(getString(R.string.room_participants_leave_prompt_msg))
            if (!isPublicRoom) {
                append("\n\n")
                append(getString(R.string.room_participants_leave_private_warning))
            }
        }
        MaterialAlertDialogBuilder(requireContext(), if (isPublicRoom) 0 else R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_leave) { _, _ ->
                    roomListViewModel.handle(RoomListAction.LeaveRoom(roomId))
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    override fun invalidate() = withState(roomListViewModel) { state ->
        footerController.setData(state)
    }

    private fun checkEmptyState() {
        val shouldShowEmpty = adapterInfosList.all { it.sectionHeaderAdapter.roomsSectionData.isHidden } &&
                !adapterInfosList.any { it.sectionHeaderAdapter.roomsSectionData.isLoading }
        if (shouldShowEmpty) {
            val emptyState = when (roomListParams.displayMode) {
                RoomListDisplayMode.NOTIFICATIONS -> {
                    StateView.State.Empty(
                            title = getString(R.string.room_list_catchup_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noun_party_popper),
                            message = getString(R.string.room_list_catchup_empty_body)
                    )
                }
                RoomListDisplayMode.PEOPLE ->
                    StateView.State.Empty(
                            title = getString(R.string.room_list_people_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_dm),
                            isBigImage = true,
                            message = getString(R.string.room_list_people_empty_body)
                    )
                RoomListDisplayMode.ROOMS ->
                    StateView.State.Empty(
                            title = getString(R.string.room_list_rooms_empty_title),
                            image = ContextCompat.getDrawable(requireContext(), R.drawable.empty_state_room),
                            isBigImage = true,
                            message = getString(R.string.room_list_rooms_empty_body)
                    )
                RoomListDisplayMode.FILTERED ->
                    // Always display the content in this mode, because if the footer
                    StateView.State.Content
            }
            views.stateView.state = emptyState
        } else {
            // is there something to show already?
            if (adapterInfosList.any { !it.sectionHeaderAdapter.roomsSectionData.isHidden }) {
                views.stateView.state = StateView.State.Content
            } else {
                views.stateView.state = StateView.State.Loading
            }
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (views.createChatFabMenu.onBackPressed()) {
            return true
        }
        return false
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomClicked(room: RoomSummary) {
        roomListViewModel.handle(RoomListAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean {
        userPreferencesProvider.neverShowLongClickOnRoomHelpAgain()
        withState(roomListViewModel) {
            // refresh footer
            footerController.setData(it)
        }
        RoomListQuickActionsBottomSheet
                .newInstance(room.roomId)
                .show(childFragmentManager, "ROOM_LIST_QUICK_ACTIONS")
        return true
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        roomListViewModel.handle(RoomListAction.AcceptInvitation(room))
    }

    override fun onJoinSuggestedRoom(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.JoinSuggestedRoom(room.childRoomId, room.viaServers))
    }

    override fun onSuggestedRoomClicked(room: SpaceChildInfo) {
        roomListViewModel.handle(RoomListAction.ShowRoomDetails(room.childRoomId, room.viaServers))
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        roomListViewModel.handle(RoomListAction.RejectInvitation(room))
    }
}
