/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.matrixto

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.util.MatrixItem

data class MatrixToBottomSheetState(
        val deepLink: String,
        val linkType: PermalinkData,
        val matrixItem: Async<MatrixItem> = Uninitialized,
        val startChattingState: Async<Unit> = Uninitialized,
        val roomPeekResult: Async<RoomInfoResult> = Uninitialized,
        val peopleYouKnow: Async<List<MatrixItem.UserItem>> = Uninitialized,
        val origin: OriginOfMatrixTo
) : MavericksState {

    constructor(args: MatrixToBottomSheet.MatrixToArgs) : this(
            deepLink = args.matrixToLink,
            linkType = PermalinkParser.parse(args.matrixToLink),
            origin = args.origin
    )
}

sealed class RoomInfoResult {
    data class FullInfo(
            val roomItem: MatrixItem,
            val name: String,
            val topic: String,
            val memberCount: Int?,
            val alias: String?,
            val membership: Membership,
            val roomType: String?,
            val viaServers: List<String>?,
            val isPublic: Boolean
    ) : RoomInfoResult()

    data class PartialInfo(
            val roomId: String?,
            val viaServers: List<String>
    ) : RoomInfoResult()

    data class UnknownAlias(
            val alias: String?
    ) : RoomInfoResult()

    object NotFound : RoomInfoResult()
}
