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
import com.google.android.material.appbar.MaterialToolbar
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.room.RequireActiveMembershipViewEvents
import im.vector.app.features.room.RequireActiveMembershipViewModel
import im.vector.app.features.room.RequireActiveMembershipViewState
import javax.inject.Inject

class RoomMemberProfileActivity :
        VectorBaseActivity<ActivitySimpleBinding>(),
        ToolbarConfigurable,
        RequireActiveMembershipViewModel.Factory {

    companion object {
        fun newIntent(context: Context, args: RoomMemberProfileArgs): Intent {
            return Intent(context, RoomMemberProfileActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, args)
            }
        }
    }

    private val requireActiveMembershipViewModel: RequireActiveMembershipViewModel by viewModel()

    @Inject
    lateinit var requireActiveMembershipViewModelFactory: RequireActiveMembershipViewModel.Factory

    override fun create(initialState: RequireActiveMembershipViewState): RequireActiveMembershipViewModel {
        return requireActiveMembershipViewModelFactory.create(initialState)
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: RoomMemberProfileArgs = intent?.extras?.getParcelable(MvRx.KEY_ARG) ?: return
            addFragment(R.id.simpleFragmentContainer, RoomMemberProfileFragment::class.java, fragmentArgs)
        }

        requireActiveMembershipViewModel.observeViewEvents {
            when (it) {
                is RequireActiveMembershipViewEvents.RoomLeft -> handleRoomLeft(it)
            }
        }
    }

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }

    private fun handleRoomLeft(roomLeft: RequireActiveMembershipViewEvents.RoomLeft) {
        if (roomLeft.leftMessage != null) {
            Toast.makeText(this, roomLeft.leftMessage, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
