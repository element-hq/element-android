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

package im.vector.app.features.spaces

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.features.spaces.create.ChooseSpaceTypeFragment
import im.vector.app.features.spaces.create.CreateSpaceAction
import im.vector.app.features.spaces.create.CreateSpaceDefaultRoomsFragment
import im.vector.app.features.spaces.create.CreateSpaceDetailsFragment
import im.vector.app.features.spaces.create.CreateSpaceEvents
import im.vector.app.features.spaces.create.CreateSpaceState
import im.vector.app.features.spaces.create.CreateSpaceViewModel
import javax.inject.Inject

class SpaceCreationActivity : SimpleFragmentActivity(), CreateSpaceViewModel.Factory {

    @Inject lateinit var viewModelFactory: CreateSpaceViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    val viewModel: CreateSpaceViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            when (withState(viewModel) { it.step }) {
                CreateSpaceState.Step.ChooseType -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceState.Step.SetDetails -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceState.Step.AddRooms -> TODO()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initUiAndData() {
        super.initUiAndData()
        viewModel.subscribe(this) {
            renderState(it)
        }

        viewModel.observeViewEvents {
            when (it) {
                CreateSpaceEvents.NavigateToDetails -> {
                    navigateToFragment(CreateSpaceDetailsFragment::class.java)
                }
                CreateSpaceEvents.NavigateToChooseType -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceEvents.Dismiss -> {
                    finish()
                }
                CreateSpaceEvents.NavigateToAddRooms -> {
                    navigateToFragment(CreateSpaceDefaultRoomsFragment::class.java)
                }
            }
        }
    }

    private fun navigateToFragment(fragmentClass: Class<out Fragment>) {
        val frag = supportFragmentManager.findFragmentByTag(fragmentClass.name) ?: createFragment(fragmentClass, Bundle().toMvRxBundle())
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.container,
                        frag,
                        fragmentClass.name
                )
                .commit()
    }

    override fun onBackPressed() {
        viewModel.handle(CreateSpaceAction.OnBackPressed)
    }

    private fun renderState(state: CreateSpaceState) {
        val titleRes = when (state.step) {
            CreateSpaceState.Step.ChooseType -> R.string.activity_create_space_title
            CreateSpaceState.Step.SetDetails -> R.string.your_public_space
            CreateSpaceState.Step.AddRooms   -> R.string.your_public_space
        }
        supportActionBar?.let {
            it.title = getString(titleRes)
        } ?: run {
            setTitle(getString(titleRes))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SpaceCreationActivity::class.java).apply {
                // putExtra(MvRx.KEY_ARG, SpaceDirectoryArgs(spaceId))
            }
        }
    }

    override fun create(initialState: CreateSpaceState): CreateSpaceViewModel = viewModelFactory.create(initialState)
}
