/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.popBackstack
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomdirectory.createroom.CreateRoomArgs
import im.vector.app.features.roomdirectory.createroom.CreateRoomFragment
import im.vector.app.features.roomdirectory.picker.RoomDirectoryPickerFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class RoomDirectoryActivity : VectorBaseActivity<ActivitySimpleBinding>(), MatrixToBottomSheet.InteractionListener {

    @Inject lateinit var roomDirectoryViewModelFactory: RoomDirectoryViewModel.Factory
    private val roomDirectoryViewModel: RoomDirectoryViewModel by viewModel()
    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomDirectory
        sharedActionViewModel = viewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)

        if (isFirstCreation()) {
            roomDirectoryViewModel.handle(RoomDirectoryAction.FilterWith(intent?.getStringExtra(INITIAL_FILTER) ?: ""))
        }

        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        is RoomDirectorySharedAction.Back -> popBackstack()
                        is RoomDirectorySharedAction.CreateRoom -> {
                            // Transmit the filter to the CreateRoomFragment
                            withState(roomDirectoryViewModel) {
                                addFragmentToBackstack(
                                        views.simpleFragmentContainer,
                                        CreateRoomFragment::class.java,
                                        CreateRoomArgs(it.currentFilter)
                                )
                            }
                        }
                        is RoomDirectorySharedAction.ChangeProtocol ->
                            addFragmentToBackstack(views.simpleFragmentContainer, RoomDirectoryPickerFragment::class.java)
                        is RoomDirectorySharedAction.Close -> finish()
                        is RoomDirectorySharedAction.CreateRoomSuccess -> Unit
                    }
                }
                .launchIn(lifecycleScope)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, PublicRoomsFragment::class.java)
        }
    }

    override fun mxToBottomSheetNavigateToRoom(roomId: String, trigger: ViewRoom.Trigger?) {
        navigator.openRoom(this, roomId, trigger = trigger)
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {
        navigator.switchToSpace(this, spaceId, Navigator.PostSwitchSpaceAction.None)
    }

    companion object {
        private const val INITIAL_FILTER = "INITIAL_FILTER"

        fun getIntent(context: Context, initialFilter: String = ""): Intent {
            val intent = Intent(context, RoomDirectoryActivity::class.java)
            intent.putExtra(INITIAL_FILTER, initialFilter)
            return intent
        }
    }
}
