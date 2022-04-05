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
 *
 */

package im.vector.app.features.roomprofile

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.room.detail.RoomDetailPendingActionStore
import im.vector.app.features.room.RequireActiveMembershipViewEvents
import im.vector.app.features.room.RequireActiveMembershipViewModel
import im.vector.app.features.roomprofile.alias.RoomAliasFragment
import im.vector.app.features.roomprofile.banned.RoomBannedMemberListFragment
import im.vector.app.features.roomprofile.members.RoomMemberListFragment
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsFragment
import im.vector.app.features.roomprofile.permissions.RoomPermissionsFragment
import im.vector.app.features.roomprofile.settings.RoomSettingsFragment
import im.vector.app.features.roomprofile.uploads.RoomUploadsFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class RoomProfileActivity :
        VectorBaseActivity<ActivitySimpleBinding>() {

    companion object {

        private const val EXTRA_DIRECT_ACCESS = "EXTRA_DIRECT_ACCESS"

        const val EXTRA_DIRECT_ACCESS_ROOM_ROOT = 0
        const val EXTRA_DIRECT_ACCESS_ROOM_SETTINGS = 1
        const val EXTRA_DIRECT_ACCESS_ROOM_MEMBERS = 2

        fun newIntent(context: Context, roomId: String, directAccess: Int?): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomProfileActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, roomProfileArgs)
                putExtra(EXTRA_DIRECT_ACCESS, directAccess)
            }
        }
    }

    private lateinit var sharedActionViewModel: RoomProfileSharedActionViewModel
    private lateinit var roomProfileArgs: RoomProfileArgs

    private val requireActiveMembershipViewModel: RequireActiveMembershipViewModel by viewModel()

    @Inject
    lateinit var roomDetailPendingActionStore: RoomDetailPendingActionStore

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        sharedActionViewModel = viewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
        roomProfileArgs = intent?.extras?.getParcelable(Mavericks.KEY_ARG) ?: return
        if (isFirstCreation()) {
            when (intent?.extras?.getInt(EXTRA_DIRECT_ACCESS, EXTRA_DIRECT_ACCESS_ROOM_ROOT)) {
                EXTRA_DIRECT_ACCESS_ROOM_SETTINGS -> {
                    addFragment(views.simpleFragmentContainer, RoomProfileFragment::class.java, roomProfileArgs)
                    addFragmentToBackstack(views.simpleFragmentContainer, RoomSettingsFragment::class.java, roomProfileArgs)
                }
                EXTRA_DIRECT_ACCESS_ROOM_MEMBERS -> {
                    addFragment(views.simpleFragmentContainer, RoomMemberListFragment::class.java, roomProfileArgs)
                }
                else -> addFragment(views.simpleFragmentContainer, RoomProfileFragment::class.java, roomProfileArgs)
            }
        }
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        RoomProfileSharedAction.OpenRoomMembers                 -> openRoomMembers()
                        RoomProfileSharedAction.OpenRoomSettings                -> openRoomSettings()
                        RoomProfileSharedAction.OpenRoomAliasesSettings         -> openRoomAlias()
                        RoomProfileSharedAction.OpenRoomPermissionsSettings     -> openRoomPermissions()
                        RoomProfileSharedAction.OpenRoomUploads                 -> openRoomUploads()
                        RoomProfileSharedAction.OpenBannedRoomMembers        -> openBannedRoomMembers()
                        RoomProfileSharedAction.OpenRoomNotificationSettings -> openRoomNotificationSettings()
                    }
                }
                .launchIn(lifecycleScope)

        requireActiveMembershipViewModel.observeViewEvents {
            when (it) {
                is RequireActiveMembershipViewEvents.RoomLeft -> handleRoomLeft(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (roomDetailPendingActionStore.data != null) {
            finish()
        }
    }

    private fun handleRoomLeft(roomLeft: RequireActiveMembershipViewEvents.RoomLeft) {
        if (roomLeft.leftMessage != null) {
            Toast.makeText(this, roomLeft.leftMessage, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun openRoomUploads() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomUploadsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomSettings() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomSettingsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomAlias() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomAliasFragment::class.java, roomProfileArgs)
    }

    private fun openRoomPermissions() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomPermissionsFragment::class.java, roomProfileArgs)
    }

    private fun openRoomMembers() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomMemberListFragment::class.java, roomProfileArgs)
    }

    private fun openBannedRoomMembers() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomBannedMemberListFragment::class.java, roomProfileArgs)
    }

    private fun openRoomNotificationSettings() {
        addFragmentToBackstack(views.simpleFragmentContainer, RoomNotificationSettingsFragment::class.java, roomProfileArgs)
    }
}
