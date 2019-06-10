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

import android.content.Context
import android.content.Intent
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotredesign.features.debug.DebugMenuActivity
import im.vector.riotredesign.features.home.room.detail.RoomDetailActivity
import im.vector.riotredesign.features.home.room.detail.RoomDetailArgs
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotredesign.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.riotredesign.features.settings.VectorSettingsActivity

class DefaultNavigator : Navigator {


    override fun openRoom(roomId: String, context: Context) {
        val args = RoomDetailArgs(roomId)
        val intent = RoomDetailActivity.newIntent(context, args)
        context.startActivity(intent)
    }

    override fun openRoomPreview(publicRoom: PublicRoom, context: Context) {
        val intent = RoomPreviewActivity.getIntent(context, publicRoom)
        context.startActivity(intent)
    }

    override fun openRoomDirectory(context: Context) {
        val intent = Intent(context, RoomDirectoryActivity::class.java)
        context.startActivity(intent)
    }

    override fun openSettings(context: Context) {
        val intent = VectorSettingsActivity.getIntent(context, "TODO")
        context.startActivity(intent)
    }

    override fun openDebug(context: Context) {
        context.startActivity(Intent(context, DebugMenuActivity::class.java))
    }
}