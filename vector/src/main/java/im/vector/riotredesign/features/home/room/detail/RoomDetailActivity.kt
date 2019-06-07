/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.room.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.platform.VectorBaseActivity

class RoomDetailActivity : VectorBaseActivity(), ToolbarConfigurable {

    override fun getLayoutRes(): Int {
        return R.layout.activity_room_detail
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFirstCreation()) {
            val roomDetailArgs: RoomDetailArgs = intent?.extras?.getParcelable(EXTRA_ROOM_DETAIL_ARGS)
                                                 ?: return
            val roomDetailFragment = RoomDetailFragment.newInstance(roomDetailArgs)
            replaceFragment(roomDetailFragment, R.id.roomDetailContainer)
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    companion object {

        private const val EXTRA_ROOM_DETAIL_ARGS = "EXTRA_ROOM_DETAIL_ARGS"

        fun newIntent(context: Context, roomDetailArgs: RoomDetailArgs): Intent {
            return Intent(context, RoomDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_DETAIL_ARGS, roomDetailArgs)
            }
        }


    }

}