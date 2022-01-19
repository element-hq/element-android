/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.e2einfo

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent

data class UserDeviceInfo(
        val userId: String,
        val deviceId: String
)

data class EncryptionInfo(
        val sentByMe: Boolean,
        val sentByThisDevice: Boolean,
        val algorithm: String?,
        val messageIndex: Int,
        val sharedWithUsers: List<UserDeviceInfo>,
        val sentByUser: UserDeviceInfo,
        val encryptedEventContent: EncryptedEventContent,
        val incomingRoomKeyRequest: List<IncomingRoomKeyRequest>,
        val locallyKnownIndex: Int? = null
)
data class CryptoInfoViewState(
        val roomId: String,
        val eventId: String,
        val timelineEvent: Async<TimelineEvent> = Uninitialized,
        val e2eInfo: Async<EncryptionInfo> = Uninitialized,
        val messageBody: CharSequence = "",
        val searchFilter: String? = null
) : MavericksState {
    constructor(args: EncryptedMessageInfoArg) : this(roomId = args.roomId, eventId = args.eventId)
}
