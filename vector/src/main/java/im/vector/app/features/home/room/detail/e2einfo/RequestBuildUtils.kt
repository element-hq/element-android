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

import android.view.View
import com.airbnb.epoxy.ModelCollector
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.Action
import im.vector.app.core.ui.list.genericItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.internal.crypto.GossipingRequestState
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest

fun ModelCollector.buildRoomKeyRequests(
        keyRequestList: List<IncomingRoomKeyRequest>,
        stringProvider: StringProvider,
        callback: CryptoInfoController.Callback?,
        vectorDateFormatter: VectorDateFormatter) {
    keyRequestList.forEach { roomKeyRequest ->

        val friendlyStatus = when (roomKeyRequest.state) {
            GossipingRequestState.NONE                   -> R.string.encryption_info_request_state_none
            GossipingRequestState.PENDING                -> R.string.encryption_info_request_state_pending
            GossipingRequestState.REJECTED               -> R.string.encryption_info_request_state_rejected
            GossipingRequestState.ACCEPTING              -> R.string.encryption_info_request_state_accepting
            GossipingRequestState.ACCEPTED               -> R.string.encryption_info_request_state_accepted
            GossipingRequestState.FAILED_TO_ACCEPTED     -> R.string.encryption_info_request_state_failed_to_accept
            GossipingRequestState.UNABLE_TO_PROCESS      -> R.string.encryption_info_request_state_unable_to_process
            GossipingRequestState.CANCELLED_BY_REQUESTER -> R.string.encryption_info_request_state_cancelled_by_requester
            GossipingRequestState.RE_REQUESTED           -> R.string.encryption_info_request_state_re_requested
        }.let {
            stringProvider.getString(it)
        }

        genericItem {
            id("req_${roomKeyRequest.hashCode()}")
            title((roomKeyRequest.localCreationTimestamp).let {
                vectorDateFormatter.format(it, DateFormatKind.MESSAGE_DETAIL).toEpoxyCharSequence()
            })
            description(
                    stringProvider.getString(
                            R.string.encryption_info_request_state_description,
                            roomKeyRequest.userId, roomKeyRequest.deviceId,
                            friendlyStatus
                    ).toEpoxyCharSequence()
            )
            apply {
                if (roomKeyRequest.state != GossipingRequestState.ACCEPTED) {
                    buttonAction(
                            Action(
                                    title = "Review Request",
                                    listener = object : ClickListener {
                                        override fun invoke(v: View) {
                                            callback?.forceShare(roomKeyRequest)
                                        }
                                    }
                            )
                    )
                }
            }
        }
    }
}
