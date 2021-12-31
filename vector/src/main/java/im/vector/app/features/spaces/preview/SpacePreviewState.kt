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

package im.vector.app.features.spaces.preview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class SpacePreviewState(
        val idOrAlias: String,
        val name: String?  = null,
        val topic: String?  = null,
        val avatarUrl: String? = null,
        val spaceInfo: Async<ChildInfo> = Uninitialized,
        val childInfoList: Async<List<ChildInfo>> = Uninitialized,
        val inviteTermination: Async<Unit> = Uninitialized
) : MavericksState {
    constructor(args: SpacePreviewArgs) : this(idOrAlias = args.idOrAlias)
}

data class ChildInfo(
        val roomId: String,
        val avatarUrl: String?,
        val name: String?,
        val topic: String?,
        val memberCount: Int?,
        val isSubSpace: Boolean?,
        val viaServers: List<String>?,
        val children: Async<List<ChildInfo>>
)
