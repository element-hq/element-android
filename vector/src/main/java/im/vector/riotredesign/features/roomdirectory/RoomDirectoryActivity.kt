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

package im.vector.riotredesign.features.roomdirectory

import android.os.Bundle
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.addFragment
import im.vector.riotredesign.core.extensions.addFragmentToBackstack
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.features.roomdirectory.createroom.CreateRoomFragment
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope

class RoomDirectoryActivity : VectorBaseActivity() {


    override fun getLayoutRes() = R.layout.activity_simple

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindScope(getOrCreateScope(RoomDirectoryModule.ROOM_DIRECTORY_SCOPE))
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(PublicRoomsFragment(), R.id.simpleFragmentContainer)
        }
    }


    fun gotoCreateRoom() {
        addFragmentToBackstack(CreateRoomFragment(), R.id.simpleFragmentContainer)
    }
}