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

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import org.matrix.android.sdk.api.session.space.peeking.ISpaceChild
import org.matrix.android.sdk.api.session.space.peeking.SpaceChildPeekResult
import org.matrix.android.sdk.api.session.space.peeking.SpacePeekResult
import org.matrix.android.sdk.api.session.space.peeking.SpacePeekSummary
import org.matrix.android.sdk.api.session.space.peeking.SpaceSubChildPeekResult
import org.matrix.android.sdk.internal.session.room.peeking.PeekRoomTask
import org.matrix.android.sdk.internal.session.room.peeking.ResolveRoomStateTask
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface PeekSpaceTask : Task<PeekSpaceTask.Params, SpacePeekResult> {
    data class Params(
            val roomIdOrAlias: String,
            // A depth limit as a simple protection against cycles
            val maxDepth: Int = 4
    )
}

internal class DefaultPeekSpaceTask @Inject constructor(
        private val peekRoomTask: PeekRoomTask,
        private val resolveRoomStateTask: ResolveRoomStateTask
) : PeekSpaceTask {

    override suspend fun execute(params: PeekSpaceTask.Params): SpacePeekResult {
        val peekResult = peekRoomTask.execute(PeekRoomTask.Params(params.roomIdOrAlias))
        val roomResult = peekResult as? PeekResult.Success ?: return SpacePeekResult.FailedToResolve(params.roomIdOrAlias, peekResult)

        // check the room type
        // kind of duplicate cause we already did it in Peek? could we pass on the result??
        val stateEvents = try {
            resolveRoomStateTask.execute(ResolveRoomStateTask.Params(roomResult.roomId))
        } catch (failure: Throwable) {
            return SpacePeekResult.FailedToResolve(params.roomIdOrAlias, peekResult)
        }
        val isSpace = stateEvents
                .lastOrNull { it.type == EventType.STATE_ROOM_CREATE && it.stateKey == "" }
                ?.content
                ?.toModel<RoomCreateContent>()
                ?.type == RoomType.SPACE

        if (!isSpace) return SpacePeekResult.NotSpaceType(params.roomIdOrAlias)

        val children = peekChildren(stateEvents, 0, params.maxDepth)

        return SpacePeekResult.Success(
                SpacePeekSummary(
                        params.roomIdOrAlias,
                        peekResult,
                        children
                )
        )
    }

    private suspend fun peekChildren(stateEvents: List<Event>, depth: Int, maxDepth: Int): List<ISpaceChild> {
        if (depth >= maxDepth) return emptyList()
        val childRoomsIds = stateEvents
                .filter {
                    it.type == EventType.STATE_SPACE_CHILD && !it.stateKey.isNullOrEmpty() &&
                            // Children where via is not present are ignored.
                            it.content?.toModel<SpaceChildContent>()?.via != null
                }
                .map { it.stateKey to it.content?.toModel<SpaceChildContent>() }

        Timber.v("## SPACE_PEEK: found ${childRoomsIds.size} present children")

        val spaceChildResults = mutableListOf<ISpaceChild>()
        childRoomsIds.forEach { entry ->

            Timber.v("## SPACE_PEEK: peeking child $entry")
            // peek each child
            val childId = entry.first ?: return@forEach
            try {
                val childPeek = peekRoomTask.execute(PeekRoomTask.Params(childId))

                val childStateEvents = resolveRoomStateTask.execute(ResolveRoomStateTask.Params(childId))
                val createContent = childStateEvents
                        .lastOrNull { it.type == EventType.STATE_ROOM_CREATE && it.stateKey == "" }
                        ?.let { it.content?.toModel<RoomCreateContent>() }

                if (!childPeek.isSuccess() || createContent == null) {
                    Timber.v("## SPACE_PEEK: cannot peek child $entry")
                    // can't peek :/
                    spaceChildResults.add(
                            SpaceChildPeekResult(
                                    childId, childPeek, entry.second?.order
                            )
                    )
                    // continue to next child
                    return@forEach
                }
                val type = createContent.type
                if (type == RoomType.SPACE) {
                    Timber.v("## SPACE_PEEK: subspace child $entry")
                    spaceChildResults.add(
                            SpaceSubChildPeekResult(
                                    childId,
                                    childPeek,
//                                    entry.second?.autoJoin,
                                    entry.second?.order,
                                    peekChildren(childStateEvents, depth + 1, maxDepth)
                            )
                    )
                } else
                /** if (type == RoomType.MESSAGING || type == null)*/
                {
                    Timber.v("## SPACE_PEEK: room child $entry")
                    spaceChildResults.add(
                            SpaceChildPeekResult(
                                    childId, childPeek, entry.second?.order
                            )
                    )
                }

                // let's check child info
            } catch (failure: Throwable) {
                // can this happen?
                Timber.e(failure, "## Failed to resolve space child")
            }
        }
        return spaceChildResults
    }
}
