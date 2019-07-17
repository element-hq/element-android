/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.roomdirectory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.viewModel
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.extensions.addFragmentToBackstack
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomFragment
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomViewModel
import im.vector.riotx.features.roomdirectory.picker.RoomDirectoryPickerFragment
import javax.inject.Inject

class RoomDirectoryActivity : VectorBaseActivity() {

    // Supported navigation actions for this Activity
    sealed class Navigation {
        object Back : Navigation()
        object CreateRoom : Navigation()
        object Close : Navigation()
        object ChangeProtocol : Navigation()
    }


    @Inject lateinit var createRoomViewModelFactory: CreateRoomViewModel.Factory
    @Inject lateinit var roomDirectoryViewModelFactory: RoomDirectoryViewModel.Factory
    private val roomDirectoryViewModel: RoomDirectoryViewModel by viewModel()
    private val createRoomViewModel: CreateRoomViewModel by viewModel()
    private lateinit var navigationViewModel: RoomDirectoryNavigationViewModel

    override fun getLayoutRes() = R.layout.activity_simple

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationViewModel = ViewModelProviders.of(this, viewModelFactory).get(RoomDirectoryNavigationViewModel::class.java)

        if (isFirstCreation()) {
            roomDirectoryViewModel.filterWith(intent?.getStringExtra(INITIAL_FILTER) ?: "")
        }

        navigationViewModel.navigateTo.observeEvent(this) { navigation ->
            when (navigation) {
                is Navigation.Back           -> onBackPressed()
                is Navigation.CreateRoom     -> addFragmentToBackstack(CreateRoomFragment(), R.id.simpleFragmentContainer)
                is Navigation.ChangeProtocol -> addFragmentToBackstack(RoomDirectoryPickerFragment(), R.id.simpleFragmentContainer)
                is Navigation.Close          -> finish()
            }
        }

        roomDirectoryViewModel.selectSubscribe(this, PublicRoomsViewState::currentFilter) { currentFilter ->
            // Transmit the filter to the createRoomViewModel
            createRoomViewModel.setName(currentFilter)
        }
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(PublicRoomsFragment(), R.id.simpleFragmentContainer)
        }
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