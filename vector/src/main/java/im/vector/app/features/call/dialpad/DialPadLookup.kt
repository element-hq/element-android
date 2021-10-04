/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.dialpad

import im.vector.app.features.call.lookup.pstnLookup
import im.vector.app.features.call.lookup.sipNativeLookup
import im.vector.app.features.call.vectorCallService
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class DialPadLookup @Inject constructor(
        private val session: Session,
        private val webRtcCallManager: WebRtcCallManager,
        private val directRoomHelper: DirectRoomHelper
) {
    sealed class Failure : Throwable() {
        object NoResult: Failure()
        object NumberIsYours: Failure()
    }

    data class Result(val userId: String, val roomId: String)

    suspend fun lookupPhoneNumber(phoneNumber: String): Result {
        session.vectorCallService.protocolChecker.awaitCheckProtocols()
        val thirdPartyUser = session.pstnLookup(phoneNumber, webRtcCallManager.supportedPSTNProtocol).firstOrNull() ?: throw Failure.NoResult
        // check to see if this is a virtual user, in which case we should find the native user
        val nativeUserId = if (webRtcCallManager.supportsVirtualRooms) {
            val nativeLookupResults = session.sipNativeLookup(thirdPartyUser.userId)
            nativeLookupResults.firstOrNull()?.userId ?: thirdPartyUser.userId
        } else {
            thirdPartyUser.userId
        }
        if (nativeUserId == session.myUserId) throw Failure.NumberIsYours
        val roomId = directRoomHelper.ensureDMExists(nativeUserId)
        return Result(userId = nativeUserId, roomId = roomId)
    }
}
