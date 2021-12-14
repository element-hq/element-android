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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Simple container for [CreateRoomFragment]
 */
@AndroidEntryPoint
class CreateRoomActivity : VectorBaseActivity<ActivitySimpleBinding>(), ToolbarConfigurable {

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(
                    views.simpleFragmentContainer,
                    CreateRoomFragment::class.java,
                    CreateRoomArgs(
                            intent?.getStringExtra(INITIAL_NAME) ?: "",
                            isSpace = intent?.getBooleanExtra(IS_SPACE, false) ?: false
                    )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = viewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        is RoomDirectorySharedAction.Back,
                        is RoomDirectorySharedAction.Close -> finish()
                    }
                }
                .launchIn(lifecycleScope)
    }

    companion object {
        private const val INITIAL_NAME = "INITIAL_NAME"
        private const val IS_SPACE = "IS_SPACE"

        fun getIntent(context: Context, initialName: String = "", isSpace: Boolean = false): Intent {
            return Intent(context, CreateRoomActivity::class.java).apply {
                putExtra(INITIAL_NAME, initialName)
                putExtra(IS_SPACE, isSpace)
            }
        }
    }
}
