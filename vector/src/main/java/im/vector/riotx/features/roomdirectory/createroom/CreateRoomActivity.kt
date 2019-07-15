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

package im.vector.riotx.features.roomdirectory.createroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotx.features.roomdirectory.RoomDirectoryNavigationViewModel

/**
 * Simple container for [CreateRoomFragment]
 */
class CreateRoomActivity : VectorBaseActivity(), ToolbarConfigurable {

    private lateinit var navigationViewModel: RoomDirectoryNavigationViewModel

    override fun getLayoutRes() = R.layout.activity_simple

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(CreateRoomFragment(), R.id.simpleFragmentContainer)
        }
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationViewModel = ViewModelProviders.of(this, viewModelFactory).get(RoomDirectoryNavigationViewModel::class.java)
        navigationViewModel.navigateTo.observeEvent(this) { navigation ->
            when (navigation) {
                is RoomDirectoryActivity.Navigation.Back -> finish()
            }
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateRoomActivity::class.java)
        }
    }

}