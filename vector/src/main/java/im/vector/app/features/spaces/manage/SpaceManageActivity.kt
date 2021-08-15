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
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomdirectory.createroom.CreateRoomArgs
import im.vector.app.features.roomdirectory.createroom.CreateRoomFragment
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.alias.RoomAliasFragment
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class SpaceManageArgs(
        val spaceId: String,
        val manageType: ManageType
) : Parcelable

class SpaceManageActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>(),
        ToolbarConfigurable,
        SpaceManageSharedViewModel.Factory {

    @Inject lateinit var sharedViewModelFactory: SpaceManageSharedViewModel.Factory
    private lateinit var sharedDirectoryActionViewModel: RoomDirectorySharedActionViewModel

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(): ActivitySimpleLoadingBinding = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    override fun getTitleRes(): Int = R.string.space_add_existing_rooms

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
            withState(sharedViewModel) {
                when (it.manageType) {
                    ManageType.AddRooms -> {
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
                    ManageType.Settings -> {
                        val simpleName = SpaceSettingsFragment::class.java.simpleName
                        if (supportFragmentManager.findFragmentByTag(simpleName) == null && args?.spaceId != null) {
                            supportFragmentManager.commitTransaction {
                                replace(R.id.simpleFragmentContainer,
                                        SpaceSettingsFragment::class.java,
                                        Bundle().apply { this.putParcelable(MvRx.KEY_ARG, RoomProfileArgs(args.spaceId)) },
                                        simpleName
                                )
                            }
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
                            R.id.simpleFragmentContainer,
                            CreateRoomFragment::class.java,
                            CreateRoomArgs("", parentSpaceId = args?.spaceId)
                    )
                }
                SpaceManagedSharedViewEvents.NavigateToManageRooms -> {
                    args?.spaceId?.let { spaceId ->
                        addFragmentToBackstack(
                                R.id.simpleFragmentContainer,
                                SpaceManageRoomsFragment::class.java,
                                SpaceManageArgs(spaceId, ManageType.ManageRooms)
                        )
                    }
                }
                SpaceManagedSharedViewEvents.NavigateToAliasSettings -> {
                    args?.spaceId?.let { spaceId ->
                        addFragmentToBackstack(
                                R.id.simpleFragmentContainer,
                                RoomAliasFragment::class.java,
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
                putExtra(MvRx.KEY_ARG, SpaceManageArgs(spaceId, manageType))
            }
        }
    }

    override fun create(initialState: SpaceManageViewState) = sharedViewModelFactory.create(initialState)

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }
}
