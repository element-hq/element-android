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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.spaces.explore.SpaceDirectoryArgs
import im.vector.app.features.spaces.explore.SpaceDirectoryFragment
import im.vector.app.features.spaces.explore.SpaceDirectoryState
import im.vector.app.features.spaces.explore.SpaceDirectoryViewEvents
import im.vector.app.features.spaces.explore.SpaceDirectoryViewModel
import javax.inject.Inject

class SpaceExploreActivity : VectorBaseActivity<ActivitySimpleBinding>(), SpaceDirectoryViewModel.Factory, MatrixToBottomSheet.InteractionListener {

    @Inject lateinit var spaceDirectoryViewModelFactory: SpaceDirectoryViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(): ActivitySimpleBinding = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getTitleRes(): Int = R.string.space_explore_activity_title

    val sharedViewModel: SpaceDirectoryViewModel by viewModel()

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = this@SpaceExploreActivity
            }
            super.onFragmentAttached(fm, f, context)
        }

        override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = null
            }
            super.onFragmentDetached(fm, f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)

        if (isFirstCreation()) {
            val simpleName = SpaceDirectoryFragment::class.java.simpleName
            val args = intent?.getParcelableExtra<SpaceDirectoryArgs>(MvRx.KEY_ARG)
            if (supportFragmentManager.findFragmentByTag(simpleName) == null) {
                supportFragmentManager.commitTransaction {
                    replace(R.id.simpleFragmentContainer,
                            SpaceDirectoryFragment::class.java,
                            Bundle().apply { this.putParcelable(MvRx.KEY_ARG, args) },
                            simpleName
                    )
                }
            }
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                SpaceDirectoryViewEvents.Dismiss -> {
                    finish()
                }
                is SpaceDirectoryViewEvents.NavigateToRoom -> {
                    navigator.openRoom(this, it.roomId)
                }
                is SpaceDirectoryViewEvents.NavigateToMxToBottomSheet -> {
                    MatrixToBottomSheet.withLink(it.link, this).show(supportFragmentManager, "ShowChild")
                }
            }
        }
    }

    override fun onDestroy() {
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    companion object {
        fun newIntent(context: Context, spaceId: String): Intent {
            return Intent(context, SpaceExploreActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, SpaceDirectoryArgs(spaceId))
            }
        }
    }

    override fun create(initialState: SpaceDirectoryState): SpaceDirectoryViewModel =
            spaceDirectoryViewModelFactory.create(initialState)

    override fun navigateToRoom(roomId: String) {
        navigator.openRoom(this, roomId)
    }
}
