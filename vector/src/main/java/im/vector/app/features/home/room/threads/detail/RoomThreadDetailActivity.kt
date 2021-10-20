/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.home.room.threads.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityRoomThreadDetailBinding

class RoomThreadDetailActivity : VectorBaseActivity<ActivityRoomThreadDetailBinding>() {

//    private val roomThreadDetailFragment: RoomThreadDetailFragment?
//        get() {
//            return supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? RoomThreadDetailFragment
//        }

    override fun getBinding() = ActivityRoomThreadDetailBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getMenuRes() = R.menu.menu_room_threads

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureToolbar(views.roomThreadDetailToolbar)
        if (isFirstCreation()) {
            val roomThreadDetailArgs: RoomThreadDetailArgs? = intent?.extras?.getParcelable(EXTRA_ROOM_THREAD_DETAIL_ARGS)
            replaceFragment(R.id.roomThreadDetailFragmentContainer, RoomThreadDetailFragment::class.java, roomThreadDetailArgs, FRAGMENT_TAG)
        }
    }

    companion object {
        private const val FRAGMENT_TAG = "RoomThreadDetailFragment"
        const val EXTRA_ROOM_THREAD_DETAIL_ARGS = "EXTRA_ROOM_THREAD_DETAIL_ARGS"

        fun newIntent(context: Context, roomThreadDetailArgs: RoomThreadDetailArgs): Intent {
            return Intent(context, RoomThreadDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_THREAD_DETAIL_ARGS, roomThreadDetailArgs)
            }
        }
    }
}
