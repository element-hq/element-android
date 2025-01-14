/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.os.Parcelable
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.verification.VerificationState
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.util.MatrixItem

@Parcelize
data class MessageInformationData(
        val eventId: String,
        val senderId: String,
        val sendState: SendState,
        val time: CharSequence? = null,
        val ageLocalTS: Long?,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val messageLayout: TimelineMessageLayout,
        val reactionsSummary: ReactionsSummaryData,
        val pollResponseAggregatedSummary: PollResponseData? = null,
        val hasBeenEdited: Boolean = false,
        val hasPendingEdits: Boolean = false,
        val referencesInfoData: ReferencesInfoData? = null,
        val sentByMe: Boolean,
        val e2eDecoration: E2EDecoration = E2EDecoration.NONE,
        val sendStateDecoration: SendStateDecoration = SendStateDecoration.NONE,
        val isFirstFromThisSender: Boolean = false,
        val isLastFromThisSender: Boolean = false,
        val messageType: String? = null
) : Parcelable {

    val matrixItem: MatrixItem
        get() = MatrixItem.UserItem(senderId, memberName?.toString(), avatarUrl)
}

@Parcelize
data class ReferencesInfoData(
        val verificationStatus: VerificationState
) : Parcelable

@Parcelize
data class ReactionsSummaryData(
        /*List of reactions (emoji,count,isSelected)*/
        val reactions: List<ReactionInfoData>? = null,
        val showAll: Boolean = false
) : Parcelable

data class ReactionsSummaryEvents(
        val onShowMoreClicked: () -> Unit,
        val onShowLessClicked: () -> Unit,
        val onAddMoreClicked: () -> Unit
)

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
        val myVote: String?,
        val votes: Map<String, PollVoteSummaryData>?,
        val totalVotes: Int = 0,
        val winnerVoteCount: Int = 0,
        val isClosed: Boolean = false,
        val hasEncryptedRelatedEvents: Boolean = false,
) : Parcelable {

    fun getVoteSummaryOfAnOption(optionId: String) = votes?.get(optionId)
}

@Parcelize
data class PollVoteSummaryData(
        val total: Int = 0,
        val percentage: Double = 0.0
) : Parcelable

enum class E2EDecoration {
    NONE,
    WARN_IN_CLEAR,
    WARN_SENT_BY_UNVERIFIED,
    WARN_SENT_BY_UNKNOWN,
    WARN_SENT_BY_DELETED_SESSION,
    WARN_UNSAFE_KEY
}

enum class SendStateDecoration {
    NONE,
    SENDING_NON_MEDIA,
    SENDING_MEDIA,
    SENT,
    FAILED
}

fun ReadReceiptData.toMatrixItem() = MatrixItem.UserItem(userId, displayName, avatarUrl)
