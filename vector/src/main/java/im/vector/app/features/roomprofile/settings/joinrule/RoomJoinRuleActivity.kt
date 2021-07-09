/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.joinrule

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedState
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedViewModel
import javax.inject.Inject

class RoomJoinRuleActivity : VectorBaseActivity<ActivitySimpleBinding>(),
        RoomJoinRuleChooseRestrictedViewModel.Factory {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    private lateinit var roomProfileArgs: RoomProfileArgs

    @Inject
    lateinit var allowListViewModelFactory: RoomJoinRuleChooseRestrictedViewModel.Factory

    override fun create(initialState: RoomJoinRuleChooseRestrictedState) = allowListViewModelFactory.create(initialState)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        roomProfileArgs = intent?.extras?.getParcelable(MvRx.KEY_ARG) ?: return
        if (isFirstCreation()) {
            addFragment(
                    R.id.simpleFragmentContainer,
                    RoomJoinRuleFragment::class.java,
                    roomProfileArgs
            )
        }
    }

    companion object {

        fun newIntent(context: Context, roomId: String): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomJoinRuleActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, roomProfileArgs)
            }
        }
    }
}
