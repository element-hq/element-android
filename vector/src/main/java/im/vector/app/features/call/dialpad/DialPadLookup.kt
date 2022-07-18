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
import timber.log.Timber
import javax.inject.Inject

class DialPadLookup @Inject constructor(
        private val session: Session,
        private val webRtcCallManager: WebRtcCallManager,
        private val directRoomHelper: DirectRoomHelper
) {
    sealed class Failure : Throwable() {
        object NoResult : Failure()
        object NumberIsYours : Failure()
    }

    data class Result(val userId: String, val roomId: String)

    suspend fun lookupPhoneNumber(phoneNumber: String): Result {
        session.vectorCallService.protocolChecker.awaitCheckProtocols()
        val thirdPartyUser = session.pstnLookup(phoneNumber, webRtcCallManager.supportedPSTNProtocol).firstOrNull() ?: throw Failure.NoResult
        val sipUserId = thirdPartyUser.userId
        val nativeLookupResults = session.sipNativeLookup(thirdPartyUser.userId)
        // If I have a native user I check for an existing native room with him...
        if (nativeLookupResults.isNotEmpty()) {
            val nativeUserId = nativeLookupResults.first().userId
            if (nativeUserId == session.myUserId) {
                throw Failure.NumberIsYours
            }
            var nativeRoomId = session.roomService().getExistingDirectRoomWithUser(nativeUserId)
            if (nativeRoomId == null) {
                // if there is no existing native room with the existing native user,
                // just create a DM with the native user
                nativeRoomId = directRoomHelper.ensureDMExists(nativeUserId)
            }
            Timber.d("lookupPhoneNumber with nativeUserId: $nativeUserId and nativeRoomId: $nativeRoomId")
            return Result(userId = nativeUserId, roomId = nativeRoomId)
        }
        // If there is no native user then we return sipUserId and sipRoomId - this is usually a PSTN call.
        val sipRoomId = directRoomHelper.ensureDMExists(sipUserId)
        Timber.d("lookupPhoneNumber with sipRoomId: $sipRoomId and sipUserId: $sipUserId")
        return Result(userId = sipUserId, roomId = sipRoomId)
    }
}
