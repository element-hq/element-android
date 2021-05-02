/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.space.peeking

import org.matrix.android.sdk.api.session.room.peeking.PeekResult

// TODO Move to api package
data class SpacePeekSummary(
        val idOrAlias: String,
        val roomPeekResult: PeekResult.Success,
        val children: List<ISpaceChild>
)

interface ISpaceChild {
    val id: String
    val roomPeekResult: PeekResult
    val default: Boolean?
    val order: String?
}

data class SpaceChildPeekResult(
        override val id: String,
        override val roomPeekResult: PeekResult,
        override val default: Boolean? = null,
        override val order: String? = null
) : ISpaceChild

data class SpaceSubChildPeekResult(
        override val id: String,
        override val roomPeekResult: PeekResult,
        override val default: Boolean?,
        override val order: String?,
        val children: List<ISpaceChild>
) : ISpaceChild

sealed class SpacePeekResult {
    abstract class SpacePeekError : SpacePeekResult()
    data class FailedToResolve(val spaceId: String, val roomPeekResult: PeekResult) : SpacePeekError()
    data class NotSpaceType(val spaceId: String) : SpacePeekError()

    data class Success(val summary: SpacePeekSummary): SpacePeekResult()
}
