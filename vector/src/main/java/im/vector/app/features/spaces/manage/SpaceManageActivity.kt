/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomdirectory.createroom.CreateRoomArgs
import im.vector.app.features.roomdirectory.createroom.CreateRoomFragment
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.alias.RoomAliasFragment
import im.vector.app.features.roomprofile.permissions.RoomPermissionsFragment
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpaceManageArgs(
        val spaceId: String,
        val manageType: ManageType
) : Parcelable

@AndroidEntryPoint
class SpaceManageActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>() {

    private lateinit var sharedDirectoryActionViewModel: RoomDirectorySharedActionViewModel

    override fun getBinding(): ActivitySimpleLoadingBinding = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    override fun getTitleRes(): Int = CommonStrings.space_add_existing_rooms

    val sharedViewModel: SpaceManageSharedViewModel by viewModel()

    override fun showWaitingView(text: String?) {
        hideKeyboard()
        views.waitingView.waitingStatusText.isGone = views.waitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView(text)
    }

    override fun hideWaitingView() {
        views.waitingView.waitingStatusText.text = null
        views.waitingView.waitingStatusText.isGone = true
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        super.hideWaitingView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedDirectoryActionViewModel = viewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        sharedDirectoryActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        is RoomDirectorySharedAction.Back,
                        is RoomDirectorySharedAction.Close -> finish()
                        else -> Unit
                    }
                }
                .launchIn(lifecycleScope)

        val args = intent?.getParcelableExtraCompat<SpaceManageArgs>(Mavericks.KEY_ARG)
        if (isFirstCreation()) {
            withState(sharedViewModel) {
                when (it.manageType) {
                    ManageType.AddRooms,
                    ManageType.AddRoomsOnlySpaces -> {
                        val simpleName = SpaceAddRoomFragment::class.java.simpleName
                        if (supportFragmentManager.findFragmentByTag(simpleName) == null) {
                            replaceFragment(
                                    views.simpleFragmentContainer,
                                    SpaceAddRoomFragment::class.java,
                                    args,
                                    simpleName
                            )
                        }
                    }
                    ManageType.Settings -> {
                        val simpleName = SpaceSettingsFragment::class.java.simpleName
                        if (supportFragmentManager.findFragmentByTag(simpleName) == null && args?.spaceId != null) {
                            replaceFragment(
                                    views.simpleFragmentContainer,
                                    SpaceSettingsFragment::class.java,
                                    RoomProfileArgs(args.spaceId),
                                    simpleName
                            )
                        }
                    }
                    ManageType.ManageRooms -> {
                        // no direct access for now
                    }
                }
            }
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                SpaceManagedSharedViewEvents.Finish -> {
                    finish()
                }
                SpaceManagedSharedViewEvents.HideLoading -> {
                    hideWaitingView()
                }
                SpaceManagedSharedViewEvents.ShowLoading -> {
                    showWaitingView()
                }
                SpaceManagedSharedViewEvents.NavigateToCreateRoom -> {
                    addFragmentToBackstack(
                            views.simpleFragmentContainer,
                            CreateRoomFragment::class.java,
                            CreateRoomArgs("", parentSpaceId = args?.spaceId)
                    )
                }
                SpaceManagedSharedViewEvents.NavigateToCreateSpace -> {
                    addFragmentToBackstack(
                            views.simpleFragmentContainer,
                            CreateRoomFragment::class.java,
                            CreateRoomArgs("", parentSpaceId = args?.spaceId, isSpace = true)
                    )
                }
                SpaceManagedSharedViewEvents.NavigateToManageRooms -> {
                    args?.spaceId?.let { spaceId ->
                        addFragmentToBackstack(
                                views.simpleFragmentContainer,
                                SpaceManageRoomsFragment::class.java,
                                SpaceManageArgs(spaceId, ManageType.ManageRooms)
                        )
                    }
                }
                SpaceManagedSharedViewEvents.NavigateToAliasSettings -> {
                    args?.spaceId?.let { spaceId ->
                        addFragmentToBackstack(
                                views.simpleFragmentContainer,
                                RoomAliasFragment::class.java,
                                RoomProfileArgs(spaceId)
                        )
                    }
                }
                SpaceManagedSharedViewEvents.NavigateToPermissionSettings -> {
                    args?.spaceId?.let { spaceId ->
                        addFragmentToBackstack(
                                views.simpleFragmentContainer, RoomPermissionsFragment::class.java,
                                RoomProfileArgs(spaceId)
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, spaceId: String, manageType: ManageType): Intent {
            return Intent(context, SpaceManageActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, SpaceManageArgs(spaceId, manageType))
            }
        }
    }
}
