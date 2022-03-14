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

package org.matrix.android.sdk.internal.session.space

import android.net.Uri
import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import org.matrix.android.sdk.api.session.space.JoinSpaceResult
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.session.space.SpaceHierarchyData
import org.matrix.android.sdk.api.session.space.SpaceService
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import org.matrix.android.sdk.api.session.space.model.SpaceParentContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.RoomGetter
import org.matrix.android.sdk.internal.session.room.SpaceGetter
import org.matrix.android.sdk.internal.session.room.create.CreateRoomTask
import org.matrix.android.sdk.internal.session.room.membership.leaving.LeaveRoomTask
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.session.space.peeking.PeekSpaceTask
import org.matrix.android.sdk.internal.session.space.peeking.SpacePeekResult
import javax.inject.Inject

internal class DefaultSpaceService @Inject constructor(
        @UserId private val userId: String,
        private val createRoomTask: CreateRoomTask,
        private val joinSpaceTask: JoinSpaceTask,
        private val spaceGetter: SpaceGetter,
        private val roomGetter: RoomGetter,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val stateEventDataSource: StateEventDataSource,
        private val peekSpaceTask: PeekSpaceTask,
        private val resolveSpaceInfoTask: ResolveSpaceInfoTask,
        private val leaveRoomTask: LeaveRoomTask
) : SpaceService {

    override suspend fun createSpace(params: CreateSpaceParams): String {
        return createRoomTask.executeRetry(params, 3)
    }

    override suspend fun createSpace(name: String, topic: String?, avatarUri: Uri?, isPublic: Boolean, roomAliasLocalPart: String?): String {
        return createSpace(CreateSpaceParams().apply {
            this.name = name
            this.topic = topic
            this.avatarUri = avatarUri
            if (isPublic) {
                this.roomAliasName = roomAliasLocalPart
                this.powerLevelContentOverride = (powerLevelContentOverride ?: PowerLevelsContent()).copy(
                        invite = if (isPublic) Role.Default.value else Role.Moderator.value
                )
                this.preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
                this.historyVisibility = RoomHistoryVisibility.WORLD_READABLE
                this.guestAccess = GuestAccess.CanJoin
            } else {
                this.preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
                visibility = RoomDirectoryVisibility.PRIVATE
            }
        })
    }

    override fun getSpace(spaceId: String): Space? {
        return spaceGetter.get(spaceId)
    }

    override fun getSpaceSummariesLive(queryParams: SpaceSummaryQueryParams,
                                       sortOrder: RoomSortOrder): LiveData<List<RoomSummary>> {
        return roomSummaryDataSource.getSpaceSummariesLive(queryParams, sortOrder)
    }

    override fun getSpaceSummaries(spaceSummaryQueryParams: SpaceSummaryQueryParams,
                                   sortOrder: RoomSortOrder): List<RoomSummary> {
        return roomSummaryDataSource.getSpaceSummaries(spaceSummaryQueryParams, sortOrder)
    }

    override fun getRootSpaceSummaries(): List<RoomSummary> {
        return roomSummaryDataSource.getRootSpaceSummaries()
    }

    override suspend fun peekSpace(spaceId: String): SpacePeekResult {
        return peekSpaceTask.execute(PeekSpaceTask.Params(spaceId))
    }

    override suspend fun querySpaceChildren(
            spaceId: String,
            suggestedOnly: Boolean?,
            limit: Int?,
            from: String?,
            knownStateList: List<Event>?
    ): SpaceHierarchyData {
        val spacesResponse = getSpacesResponse(spaceId, suggestedOnly, limit, from)
        val spaceRootResponse = spacesResponse.getRoot(spaceId)
        val spaceRoot = spaceRootResponse?.toRoomSummary() ?: createBlankRoomSummary(spaceId)
        val spaceChildren = spacesResponse.rooms.mapSpaceChildren(spaceId, spaceRootResponse, knownStateList)

        return SpaceHierarchyData(
                rootSummary = spaceRoot,
                children = spaceChildren,
                childrenState = spaceRootResponse?.childrenState.orEmpty(),
                nextToken = spacesResponse.nextBatch
        )
    }

    private suspend fun getSpacesResponse(spaceId: String, suggestedOnly: Boolean?, limit: Int?, from: String?) =
            resolveSpaceInfoTask.execute(
                    ResolveSpaceInfoTask.Params(spaceId = spaceId, limit = limit, maxDepth = 1, from = from, suggestedOnly = suggestedOnly)
            )

    private fun SpacesResponse.getRoot(spaceId: String) = rooms?.firstOrNull { it.roomId == spaceId }

    private fun SpaceChildSummaryResponse.toRoomSummary() = RoomSummary(
            roomId = roomId,
            roomType = roomType,
            name = name ?: "",
            displayName = name ?: "",
            topic = topic ?: "",
            joinedMembersCount = numJoinedMembers,
            avatarUrl = avatarUrl ?: "",
            encryptionEventTs = null,
            typingUsers = emptyList(),
            isEncrypted = false,
            flattenParentIds = emptyList(),
            canonicalAlias = canonicalAlias,
            joinRules = RoomJoinRules.PUBLIC.takeIf { isWorldReadable }
    )

    private fun createBlankRoomSummary(spaceId: String) = RoomSummary(
            roomId = spaceId,
            joinedMembersCount = null,
            encryptionEventTs = null,
            typingUsers = emptyList(),
            isEncrypted = false,
            flattenParentIds = emptyList(),
            canonicalAlias = null,
            joinRules = null
    )

    private fun List<SpaceChildSummaryResponse>?.mapSpaceChildren(
            spaceId: String,
            spaceRootResponse: SpaceChildSummaryResponse?,
            knownStateList: List<Event>?,
    ) = this?.filterIdIsNot(spaceId)
            ?.toSpaceChildInfoList(spaceId, spaceRootResponse, knownStateList)
            .orEmpty()

    private fun List<SpaceChildSummaryResponse>.filterIdIsNot(spaceId: String) = filter { it.roomId != spaceId }

    private fun List<SpaceChildSummaryResponse>.toSpaceChildInfoList(
            spaceId: String,
            rootRoomResponse: SpaceChildSummaryResponse?,
            knownStateList: List<Event>?,
    ) = flatMap { spaceChildSummary ->
        (rootRoomResponse?.childrenState ?: knownStateList)
                ?.filter { it.isChildOf(spaceChildSummary) }
                ?.mapNotNull { childStateEvent -> childStateEvent.toSpaceChildInfo(spaceId, spaceChildSummary) }
                .orEmpty()
    }

    private fun Event.isChildOf(space: SpaceChildSummaryResponse) = stateKey == space.roomId && type == EventType.STATE_SPACE_CHILD

    private fun Event.toSpaceChildInfo(spaceId: String, summary: SpaceChildSummaryResponse) = content.toModel<SpaceChildContent>()?.let { content ->
        createSpaceChildInfo(spaceId, summary, content)
    }

    private fun createSpaceChildInfo(
            spaceId: String,
            summary: SpaceChildSummaryResponse,
            content: SpaceChildContent
    ) = SpaceChildInfo(
            childRoomId = summary.roomId,
            isKnown = true,
            roomType = summary.roomType,
            name = summary.name,
            topic = summary.topic,
            avatarUrl = summary.avatarUrl,
            order = content.order,
            viaServers = content.via.orEmpty(),
            activeMemberCount = summary.numJoinedMembers,
            parentRoomId = spaceId,
            suggested = content.suggested,
            canonicalAlias = summary.canonicalAlias,
            aliases = summary.aliases,
            worldReadable = summary.isWorldReadable
    )

    override suspend fun joinSpace(spaceIdOrAlias: String,
                                   reason: String?,
                                   viaServers: List<String>): JoinSpaceResult {
        return joinSpaceTask.execute(JoinSpaceTask.Params(spaceIdOrAlias, reason, viaServers))
    }

    override suspend fun leaveSpace(spaceId: String, reason: String?) {
        leaveRoomTask.execute(LeaveRoomTask.Params(spaceId, reason))
    }

    override suspend fun rejectInvite(spaceId: String, reason: String?) {
        leaveRoomTask.execute(LeaveRoomTask.Params(spaceId, reason))
    }

    override suspend fun setSpaceParent(childRoomId: String, parentSpaceId: String, canonical: Boolean, viaServers: List<String>) {
        // Should we perform some validation here?,
        // and if client want to bypass, it could use sendStateEvent directly?
        if (canonical) {
            // check that we can send m.child in the parent room
            if (roomSummaryDataSource.getRoomSummary(parentSpaceId)?.membership != Membership.JOIN) {
                throw UnsupportedOperationException("Cannot add canonical child if not member of parent")
            }
            val powerLevelsEvent = stateEventDataSource.getStateEvent(
                    roomId = parentSpaceId,
                    eventType = EventType.STATE_ROOM_POWER_LEVELS,
                    stateKey = QueryStringValue.NoCondition
            )
            val powerLevelsContent = powerLevelsEvent?.content?.toModel<PowerLevelsContent>()
                    ?: throw UnsupportedOperationException("Cannot add canonical child, missing powerlevel")
            val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
            if (!powerLevelsHelper.isUserAllowedToSend(userId, true, EventType.STATE_SPACE_CHILD)) {
                throw UnsupportedOperationException("Cannot add canonical child, not enough power level")
            }
        }

        val room = roomGetter.getRoom(childRoomId)
                ?: throw IllegalArgumentException("Unknown Room $childRoomId")

        room.sendStateEvent(
                eventType = EventType.STATE_SPACE_PARENT,
                stateKey = parentSpaceId,
                body = SpaceParentContent(
                        via = viaServers,
                        canonical = canonical
                ).toContent()
        )
    }

    override suspend fun removeSpaceParent(childRoomId: String, parentSpaceId: String) {
        val room = roomGetter.getRoom(childRoomId)
                ?: throw IllegalArgumentException("Unknown Room $childRoomId")

        val existingEvent = room.getStateEvent(EventType.STATE_SPACE_PARENT, QueryStringValue.Equals(parentSpaceId))
        if (existingEvent != null) {
            // Should i check if it was sent by me?
            // we don't check power level, it will throw if you cannot do that
            room.sendStateEvent(
                    eventType = EventType.STATE_SPACE_PARENT,
                    stateKey = parentSpaceId,
                    body = SpaceParentContent(
                            via = null,
                            canonical = null
                    ).toContent()
            )
        }
    }
}
