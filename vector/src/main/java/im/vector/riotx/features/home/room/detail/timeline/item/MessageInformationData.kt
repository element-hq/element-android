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

package im.vector.riotx.features.home.room.detail.timeline.item

import android.os.Parcelable
import im.vector.matrix.android.api.session.room.send.SendState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MessageInformationData(
        val eventId: String,
        val senderId: String,
        val sendState: SendState,
        val time: CharSequence? = null,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true,
        /*List of reactions (emoji,count,isSelected)*/
        val orderedReactionList: List<ReactionInfoData>? = null,
        val hasBeenEdited: Boolean = false,
        val hasPendingEdits: Boolean = false,
        val readReceipts: List<ReadReceiptData> = emptyList()
) : Parcelable

@Parcelize
data class ReactionInfoData(
        val key: String,
        val count: Int,
        val addedByMe: Boolean,
        val synced: Boolean
) : Parcelable

@Parcelize
data class ReadReceiptData(
        val userId: String,
        val avatarUrl: String?,
        val displayName: String?,
        val timestamp: Long
) : Parcelable
