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
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.threads.detail.arguments.RoomThreadDetailArgs
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

class RoomThreadDetailActivity : VectorBaseActivity<ActivityRoomThreadDetailBinding>() {

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

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
        initToolbar()
        initFragment()
    }

    private fun initToolbar() {
        configureToolbar(views.roomThreadDetailToolbar)
        getRoomThreadDetailArgs()?.let {
            val matrixItem = MatrixItem.RoomItem(it.roomId, it.displayName, it.avatarUrl)
            avatarRenderer.render(matrixItem, views.roomThreadDetailToolbarImageView)
        }
    }

    private fun initFragment() {
        if (isFirstCreation()) {
            replaceFragment(R.id.roomThreadDetailFragmentContainer, RoomThreadDetailFragment::class.java, getRoomThreadDetailArgs(), FRAGMENT_TAG)
        }
    }

    private fun getRoomThreadDetailArgs(): RoomThreadDetailArgs? = intent?.extras?.getParcelable(ROOM_THREAD_DETAIL_ARGS)

    companion object {
        private val FRAGMENT_TAG = RoomThreadDetailFragment::class.simpleName
        const val ROOM_THREAD_DETAIL_ARGS = "ROOM_THREAD_DETAIL_ARGS"

        fun newIntent(context: Context, roomThreadDetailArgs: RoomThreadDetailArgs): Intent {
            return Intent(context, RoomThreadDetailActivity::class.java).apply {
                putExtra(ROOM_THREAD_DETAIL_ARGS, roomThreadDetailArgs)
            }
        }
    }
}
