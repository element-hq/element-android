/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.alias

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomAliasViewState(
        val roomId: String,
        val homeServerName: String = "",
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val actionPermissions: ActionPermissions = ActionPermissions(),
        val isLoading: Boolean = false,
        val canonicalAlias: String? = null,
        val alternativeAliases: List<String> = emptyList(),
        val newAlias: String = "",
        val localAliases: Async<List<String>> = Uninitialized,
        val newLocalAlias: String = "",
        val asyncNewLocalAliasRequest: Async<Unit> = Uninitialized
) : MvRxState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    data class ActionPermissions(
            val canChangeCanonicalAlias: Boolean = false
    )
}
