/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail

import android.net.Uri
import android.view.View
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.call.webrtc.WebRtcCall
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.util.MatrixItem
import java.io.File

/**
 * Transient events for RoomDetail
 */
sealed class RoomDetailViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable, val showInDialog: Boolean = false) : RoomDetailViewEvents()
    data class OnNewTimelineEvents(val eventIds: List<String>) : RoomDetailViewEvents()

    data class ActionSuccess(val action: RoomDetailAction) : RoomDetailViewEvents()
    data class ActionFailure(val action: RoomDetailAction, val throwable: Throwable) : RoomDetailViewEvents()

    data class ShowMessage(val message: String) : RoomDetailViewEvents()
    data class ShowInfoOkDialog(val message: String) : RoomDetailViewEvents()
    data class ShowE2EErrorMessage(val withHeldCode: WithHeldCode?) : RoomDetailViewEvents()

    data class OpenRoom(val roomId: String, val closeCurrentRoom: Boolean = false) : RoomDetailViewEvents()

    data class NavigateToEvent(val eventId: String) : RoomDetailViewEvents()
    data class JoinJitsiConference(val widget: Widget, val withVideo: Boolean) : RoomDetailViewEvents()
    object LeaveJitsiConference : RoomDetailViewEvents()

    object OpenInvitePeople : RoomDetailViewEvents()
    object OpenSetRoomAvatarDialog : RoomDetailViewEvents()
    object OpenRoomSettings : RoomDetailViewEvents()
    object OpenRoomProfile : RoomDetailViewEvents()
    data class ShowRoomAvatarFullScreen(val matrixItem: MatrixItem?, val view: View?) : RoomDetailViewEvents()

    object ShowWaitingView : RoomDetailViewEvents()
    object HideWaitingView : RoomDetailViewEvents()

    data class DownloadFileState(
            val mimeType: String?,
            val file: File?,
            val throwable: Throwable?
    ) : RoomDetailViewEvents()

    data class OpenFile(
            val uri: Uri,
            val mimeType: String?
    ) : RoomDetailViewEvents()

    data class DisplayAndAcceptCall(val call: WebRtcCall) : RoomDetailViewEvents()

    object DisplayPromptForIntegrationManager : RoomDetailViewEvents()

    object DisplayEnableIntegrationsWarning : RoomDetailViewEvents()

    data class OpenStickerPicker(val widget: Widget) : RoomDetailViewEvents()

    object OpenIntegrationManager : RoomDetailViewEvents()
    object OpenActiveWidgetBottomSheet : RoomDetailViewEvents()
    data class RequestNativeWidgetPermission(val widget: Widget,
                                             val domain: String,
                                             val grantedEvents: RoomDetailViewEvents) : RoomDetailViewEvents()

    data class StartChatEffect(val type: ChatEffect) : RoomDetailViewEvents()
    object StopChatEffects : RoomDetailViewEvents()
    object RoomReplacementStarted : RoomDetailViewEvents()

    data class ChangeLocationIndicator(val isVisible: Boolean) : RoomDetailViewEvents()
}
