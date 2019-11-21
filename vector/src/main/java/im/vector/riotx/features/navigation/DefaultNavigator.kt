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

package im.vector.riotx.features.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotx.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.riotx.features.debug.DebugMenuActivity
import im.vector.riotx.features.home.createdirect.CreateDirectRoomActivity
import im.vector.riotx.features.home.room.detail.RoomDetailActivity
import im.vector.riotx.features.home.room.detail.RoomDetailArgs
import im.vector.riotx.features.home.room.filtered.FilteredRoomsActivity
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.riotx.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.riotx.features.settings.VectorSettingsActivity
import im.vector.riotx.features.share.SharedData
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.app.TaskStackBuilder


@Singleton
class DefaultNavigator @Inject constructor() : Navigator {

    override fun openRoom(context: Context, roomId: String, eventId: String?, buildTask: Boolean) {
        val args = RoomDetailArgs(roomId, eventId)
        val intent = RoomDetailActivity.newIntent(context, args)
        if (buildTask) {
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntentWithParentStack(intent)
            stackBuilder.startActivities()
        } else {
            context.startActivity(intent)
        }
    }

    override fun openNotJoinedRoom(context: Context, roomIdOrAlias: String, eventId: String?, buildTask: Boolean) {
        if (context is VectorBaseActivity) {
            context.notImplemented("Open not joined room")
        } else {
            context.toast(R.string.not_implemented)
        }
    }


    override fun openGroupDetail(groupId: String, context: Context, buildTask: Boolean) {
        Timber.v("Open group detail $groupId")
    }

    override fun openUserDetail(userId: String, context: Context, buildTask: Boolean) {
        Timber.v("Open user detail $userId")
    }

    override fun openRoomForSharing(activity: Activity, roomId: String, sharedData: SharedData) {
        val args = RoomDetailArgs(roomId, null, sharedData)
        val intent = RoomDetailActivity.newIntent(activity, args)
        activity.startActivity(intent)
        activity.finish()
    }

    override fun openRoomPreview(publicRoom: PublicRoom, context: Context) {
        val intent = RoomPreviewActivity.getIntent(context, publicRoom)
        context.startActivity(intent)
    }

    override fun openRoomDirectory(context: Context, initialFilter: String) {
        val intent = RoomDirectoryActivity.getIntent(context, initialFilter)
        context.startActivity(intent)
    }

    override fun openCreateRoom(context: Context, initialName: String) {
        val intent = CreateRoomActivity.getIntent(context, initialName)
        context.startActivity(intent)
    }

    override fun openCreateDirectRoom(context: Context) {
        val intent = CreateDirectRoomActivity.getIntent(context)
        context.startActivity(intent)
    }

    override fun openRoomsFiltering(context: Context) {
        val intent = FilteredRoomsActivity.newIntent(context)
        context.startActivity(intent)
    }

    override fun openSettings(context: Context) {
        val intent = VectorSettingsActivity.getIntent(context)
        context.startActivity(intent)
    }

    override fun openDebug(context: Context) {
        context.startActivity(Intent(context, DebugMenuActivity::class.java))
    }

    override fun openKeysBackupSetup(context: Context, showManualExport: Boolean) {
        context.startActivity(KeysBackupSetupActivity.intent(context, showManualExport))
    }

    override fun openKeysBackupManager(context: Context) {
        context.startActivity(KeysBackupManageActivity.intent(context))
    }

    override fun openRoomSettings(context: Context, roomId: String) {
        Timber.v("Open room settings$roomId")
    }
}
