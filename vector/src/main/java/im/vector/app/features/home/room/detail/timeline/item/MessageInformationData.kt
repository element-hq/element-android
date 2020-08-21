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

package im.vector.app.features.home.room.detail.timeline.item

import android.os.Parcelable
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.internal.session.room.VerificationState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MessageInformationData(
        val eventId: String,
        val senderId: String,
        val sendState: SendState,
        val time: CharSequence? = null,
        val ageLocalTS : Long?,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true,
        /*List of reactions (emoji,count,isSelected)*/
        val orderedReactionList: List<ReactionInfoData>? = null,
        val pollResponseAggregatedSummary: PollResponseData? = null,

        val hasBeenEdited: Boolean = false,
        val hasPendingEdits: Boolean = false,
        val readReceipts: List<ReadReceiptData> = emptyList(),
        val referencesInfoData: ReferencesInfoData? = null,
        val sentByMe : Boolean,
        val e2eDecoration: E2EDecoration = E2EDecoration.NONE
) : Parcelable {

    val matrixItem: MatrixItem
        get() = MatrixItem.UserItem(senderId, memberName?.toString(), avatarUrl)
}

@Parcelize
data class ReferencesInfoData(
        val verificationStatus: VerificationState
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

@Parcelize
data class PollResponseData(
        val myVote: Int?,
        val votes: Map<Int, Int>?,
        val isClosed: Boolean = false
) : Parcelable

enum class E2EDecoration {
    NONE,
    WARN_IN_CLEAR,
    WARN_SENT_BY_UNVERIFIED,
    WARN_SENT_BY_UNKNOWN
}

fun ReadReceiptData.toMatrixItem() = MatrixItem.UserItem(userId, displayName, avatarUrl)
