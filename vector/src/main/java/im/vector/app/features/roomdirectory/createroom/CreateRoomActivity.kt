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

package im.vector.app.features.roomdirectory.createroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import javax.inject.Inject

/**
 * Simple container for [CreateRoomFragment]
 */
class CreateRoomActivity : VectorBaseActivity(), ToolbarConfigurable {

    @Inject lateinit var createRoomViewModelFactory: CreateRoomViewModel.Factory
    private val createRoomViewModel: CreateRoomViewModel by viewModel()

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel

    override fun getLayoutRes() = R.layout.activity_simple

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(R.id.simpleFragmentContainer, CreateRoomFragment::class.java)
            createRoomViewModel.handle(CreateRoomAction.SetName(intent?.getStringExtra(INITIAL_NAME) ?: ""))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = viewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        is RoomDirectorySharedAction.Back,
                        is RoomDirectorySharedAction.Close -> finish()
                    }
                }
                .disposeOnDestroy()
    }

    companion object {
        private const val INITIAL_NAME = "INITIAL_NAME"

        fun getIntent(context: Context, initialName: String = ""): Intent {
            return Intent(context, CreateRoomActivity::class.java).apply {
                putExtra(INITIAL_NAME, initialName)
            }
        }
    }
}
