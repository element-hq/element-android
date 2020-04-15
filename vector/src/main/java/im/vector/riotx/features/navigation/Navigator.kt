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
import android.view.View
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.riotx.features.settings.VectorSettingsActivity
import im.vector.riotx.features.share.SharedData

interface Navigator {

    fun openRoom(context: Context, roomId: String, eventId: String? = null, buildTask: Boolean = false)

    fun performDeviceVerification(context: Context, otherUserId: String, sasTransactionId: String)

    fun requestSessionVerification(context: Context, otherSessionId: String)

    fun waitSessionVerification(context: Context)

    fun openRoomForSharingAndFinish(activity: Activity, roomId: String, sharedData: SharedData)

    fun openNotJoinedRoom(context: Context, roomIdOrAlias: String?, eventId: String? = null, buildTask: Boolean = false)

    fun openRoomPreview(publicRoom: PublicRoom, context: Context)

    fun openCreateRoom(context: Context, initialName: String = "")

    fun openCreateDirectRoom(context: Context)

    fun openRoomDirectory(context: Context, initialFilter: String = "")

    fun openRoomsFiltering(context: Context)

    fun openSettings(context: Context, directAccess: Int = VectorSettingsActivity.EXTRA_DIRECT_ACCESS_ROOT)

    fun openDebug(context: Context)

    fun openKeysBackupSetup(context: Context, showManualExport: Boolean)

    fun openKeysBackupManager(context: Context)

    fun openGroupDetail(groupId: String, context: Context, buildTask: Boolean = false)

    fun openRoomMemberProfile(userId: String, roomId: String?, context: Context, buildTask: Boolean = false)

    fun openRoomProfile(context: Context, roomId: String)

    fun openBigImageViewer(activity: Activity, sharedElement: View?, matrixItem: MatrixItem)
}
