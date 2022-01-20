/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roommemberprofile

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.room.RequireActiveMembershipViewEvents
import im.vector.app.features.room.RequireActiveMembershipViewModel

@AndroidEntryPoint
class RoomMemberProfileActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    companion object {
        fun newIntent(context: Context, args: RoomMemberProfileArgs): Intent {
            return Intent(context, RoomMemberProfileActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }

    private val requireActiveMembershipViewModel: RequireActiveMembershipViewModel by viewModel()

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: RoomMemberProfileArgs = intent?.extras?.getParcelable(Mavericks.KEY_ARG) ?: return
            addFragment(views.simpleFragmentContainer, RoomMemberProfileFragment::class.java, fragmentArgs)
        }

        requireActiveMembershipViewModel.observeViewEvents {
            when (it) {
                is RequireActiveMembershipViewEvents.RoomLeft -> handleRoomLeft(it)
            }
        }
    }

    private fun handleRoomLeft(roomLeft: RequireActiveMembershipViewEvents.RoomLeft) {
        if (roomLeft.leftMessage != null) {
            Toast.makeText(this, roomLeft.leftMessage, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
