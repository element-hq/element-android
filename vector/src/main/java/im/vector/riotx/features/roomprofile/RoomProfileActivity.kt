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

package im.vector.riotx.features.roomprofile

import android.content.Context
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import im.vector.riotx.R
import im.vector.riotx.core.extensions.commitTransaction
import im.vector.riotx.core.extensions.commitTransactionNow
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.roomprofile.members.RoomMemberListFragment

class RoomProfileActivity : VectorBaseActivity(), ToolbarConfigurable {

    companion object {

        private const val EXTRA_ROOM_PROFILE_ARGS = "EXTRA_ROOM_PROFILE_ARGS"
        private const val TAG_ROOM_PROFILE_FRAGMENT = "TAG_ROOM_PROFILE_FRAGMENT"
        private const val TAG_ROOM_MEMBER_LIST_FRAGMENT = "TAG_ROOM_MEMBER_LIST_FRAGMENT"


        fun newIntent(context: Context, roomId: String): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomProfileActivity::class.java).apply {
                putExtra(EXTRA_ROOM_PROFILE_ARGS, roomProfileArgs)
            }
        }
    }

    private lateinit var sharedActionViewModel: RoomProfileSharedActionViewModel
    private lateinit var roomProfileArgs: RoomProfileArgs

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        sharedActionViewModel = viewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
        roomProfileArgs = intent?.extras?.getParcelable(EXTRA_ROOM_PROFILE_ARGS) ?: return
        if (isFirstCreation()) {
            val argsBundle = roomProfileArgs.toMvRxBundle()
            val roomProfileFragment = createFragment(RoomProfileFragment::class.java, argsBundle)
            val roomMemberListFragment = createFragment(RoomMemberListFragment::class.java, argsBundle)
            supportFragmentManager.commitTransactionNow {
                add(R.id.simpleFragmentContainer, roomProfileFragment, TAG_ROOM_PROFILE_FRAGMENT)
                add(R.id.simpleFragmentContainer, roomMemberListFragment, TAG_ROOM_MEMBER_LIST_FRAGMENT)
                detach(roomMemberListFragment)
            }
        }
        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        is RoomProfileSharedAction.OpenRoomMembers  -> openRoomMembers()
                        is RoomProfileSharedAction.OpenRoomSettings -> openRoomSettings()
                        is RoomProfileSharedAction.OpenRoomUploads  -> openRoomUploads()
                    }
                }
                .disposeOnDestroy()
    }

    private fun openRoomUploads() {
        notImplemented("Open room uploads")
    }

    private fun openRoomSettings() {
        notImplemented("Open room settings")
    }

    private fun openRoomMembers() {
        val roomProfileFragment = supportFragmentManager.findFragmentByTag(TAG_ROOM_PROFILE_FRAGMENT)
                                  ?: throw IllegalStateException("You should have a RoomProfileFragment")
        val roomMemberListFragment = supportFragmentManager.findFragmentByTag(TAG_ROOM_MEMBER_LIST_FRAGMENT)
                                     ?: throw IllegalStateException("You should have a RoomMemberListFragment")
        supportFragmentManager.commitTransaction {
            hide(roomProfileFragment)
            attach(roomMemberListFragment)
            addToBackStack(null)
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

}
