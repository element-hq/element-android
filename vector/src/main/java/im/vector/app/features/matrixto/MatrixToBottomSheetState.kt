/*
 * Copyright (c) 2020 New Vector Ltd
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
        val peopleYouKnow: Async<List<MatrixItem.UserItem>> = Uninitialized
) : MavericksState {

    constructor(args: MatrixToBottomSheet.MatrixToArgs) : this(
            deepLink = args.matrixToLink,
            linkType = PermalinkParser.parse(args.matrixToLink)
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
