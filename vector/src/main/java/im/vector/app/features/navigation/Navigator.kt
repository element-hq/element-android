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

package im.vector.app.features.navigation

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import im.vector.app.features.home.room.detail.widget.WidgetRequestCodes
import im.vector.app.features.media.AttachmentData
import im.vector.app.features.pin.PinActivity
import im.vector.app.features.pin.PinMode
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.share.SharedData
import im.vector.app.features.terms.ReviewTermsActivity
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.session.room.model.thirdparty.RoomDirectoryData
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.util.MatrixItem

interface Navigator {

    fun openRoom(context: Context, roomId: String, eventId: String? = null, buildTask: Boolean = false)

    fun performDeviceVerification(context: Context, otherUserId: String, sasTransactionId: String)

    fun requestSessionVerification(context: Context, otherSessionId: String)

    fun requestSelfSessionVerification(context: Context)

    fun waitSessionVerification(context: Context)

    fun upgradeSessionSecurity(context: Context, initCrossSigningOnly: Boolean)

    fun openRoomForSharingAndFinish(activity: Activity, roomId: String, sharedData: SharedData)

    fun openNotJoinedRoom(context: Context, roomIdOrAlias: String?, eventId: String? = null, buildTask: Boolean = false)

    fun openRoomPreview(context: Context, publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData)

    fun openCreateRoom(context: Context, initialName: String = "")

    fun openCreateDirectRoom(context: Context)

    fun openInviteUsersToRoom(context: Context, roomId: String)

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

    fun openPinCode(fragment: Fragment, pinMode: PinMode, requestCode: Int = PinActivity.PIN_REQUEST_CODE)

    fun openPinCode(activity: Activity, pinMode: PinMode, requestCode: Int = PinActivity.PIN_REQUEST_CODE)

    fun openTerms(fragment: Fragment,
                  serviceType: TermsService.ServiceType,
                  baseUrl: String,
                  token: String?,
                  requestCode: Int = ReviewTermsActivity.TERMS_REQUEST_CODE)

    fun openStickerPicker(fragment: Fragment,
                          roomId: String,
                          widget: Widget,
                          requestCode: Int = WidgetRequestCodes.STICKER_PICKER_REQUEST_CODE)

    fun openIntegrationManager(fragment: Fragment, roomId: String, integId: String?, screen: String?)

    fun openRoomWidget(context: Context, roomId: String, widget: Widget, options: Map<String, Any>? = null)

    fun openMediaViewer(activity: Activity,
                        roomId: String,
                        mediaData: AttachmentData,
                        view: View,
                        inMemory: List<AttachmentData> = emptyList(),
                        options: ((MutableList<Pair<View, String>>) -> Unit)?)
}
