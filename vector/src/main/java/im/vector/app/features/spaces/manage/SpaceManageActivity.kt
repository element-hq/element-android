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

package im.vector.app.features.spaces.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.view.isVisible
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomdirectory.createroom.CreateRoomArgs
import im.vector.app.features.roomdirectory.createroom.CreateRoomFragment
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class SpaceManageArgs(
        val spaceId: String
) : Parcelable

class SpaceManageActivity : VectorBaseActivity<ActivitySimpleBinding>(), SpaceManageSharedViewModel.Factory {

    @Inject lateinit var sharedViewModelFactory: SpaceManageSharedViewModel.Factory
    private lateinit var sharedDirectoryActionViewModel: RoomDirectorySharedActionViewModel

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(): ActivitySimpleBinding = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getTitleRes(): Int = R.string.space_add_existing_rooms

    val sharedViewModel: SpaceManageSharedViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedDirectoryActionViewModel = viewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        sharedDirectoryActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        is RoomDirectorySharedAction.Back,
                        is RoomDirectorySharedAction.Close -> finish()
                    }
                }
                .disposeOnDestroy()

        val args = intent?.getParcelableExtra<SpaceManageArgs>(MvRx.KEY_ARG)
        if (isFirstCreation()) {
            val simpleName = SpaceAddRoomFragment::class.java.simpleName
            if (supportFragmentManager.findFragmentByTag(simpleName) == null) {
                supportFragmentManager.commitTransaction {
                    replace(R.id.simpleFragmentContainer,
                            SpaceAddRoomFragment::class.java,
                            Bundle().apply { this.putParcelable(MvRx.KEY_ARG, args) },
                            simpleName
                    )
                }
            }
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                SpaceManagedSharedViewEvents.Finish -> {
                    finish()
                }
                SpaceManagedSharedViewEvents.HideLoading -> {
                    views.simpleActivityWaitingView.isVisible = false
                }
                SpaceManagedSharedViewEvents.ShowLoading -> {
                    views.simpleActivityWaitingView.isVisible = true
                }
                SpaceManagedSharedViewEvents.NavigateToCreateRoom -> {
                    addFragmentToBackstack(
                            R.id.simpleFragmentContainer,
                            CreateRoomFragment::class.java,
                            CreateRoomArgs("", parentSpaceId = args?.spaceId)
                    )
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, spaceId: String): Intent {
            return Intent(context, SpaceManageActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, SpaceManageArgs(spaceId))
            }
        }
    }

    override fun create(initialState: SpaceManageViewState) = sharedViewModelFactory.create(initialState)
}
