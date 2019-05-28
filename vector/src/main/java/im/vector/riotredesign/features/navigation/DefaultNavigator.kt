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

package im.vector.riotredesign.features.navigation

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotredesign.features.home.room.detail.RoomDetailActivity
import im.vector.riotredesign.features.home.room.detail.RoomDetailArgs
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotredesign.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.riotredesign.features.settings.VectorSettingsActivity

class DefaultNavigator(private val fraqment: Fragment) : Navigator {

    val activity: Activity = fraqment.requireActivity()

    override fun openRoom(roomId: String) {
        val args = RoomDetailArgs(roomId)
        val intent = RoomDetailActivity.newIntent(activity, args)
        activity.startActivity(intent)
    }

    override fun openRoomPreview(publicRoom: PublicRoom) {
        val intent = RoomPreviewActivity.getIntent(activity, publicRoom)
        activity.startActivity(intent)
    }

    override fun openRoomDirectory() {
        val intent = Intent(activity, RoomDirectoryActivity::class.java)
        activity.startActivity(intent)
    }

    override fun openSettings() {
        val intent = VectorSettingsActivity.getIntent(activity, "TODO")
        activity.startActivity(intent)
    }
}