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

package im.vector.matrix.android.internal.session.room

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.session.DefaultFileService
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.create.CreateRoomTask
import im.vector.matrix.android.internal.session.room.create.DefaultCreateRoomTask
import im.vector.matrix.android.internal.session.room.directory.DefaultGetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.DefaultGetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.directory.GetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.GetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.membership.DefaultLoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.membership.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.membership.joining.DefaultInviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.DefaultJoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.joining.InviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.DefaultLeaveRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.LeaveRoomTask
import im.vector.matrix.android.internal.session.room.prune.DefaultPruneEventTask
import im.vector.matrix.android.internal.session.room.prune.PruneEventTask
import im.vector.matrix.android.internal.session.room.read.DefaultMarkAllRoomsReadTask
import im.vector.matrix.android.internal.session.room.read.DefaultSetReadMarkersTask
import im.vector.matrix.android.internal.session.room.read.MarkAllRoomsReadTask
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.session.room.relation.*
import im.vector.matrix.android.internal.session.room.state.DefaultSendStateTask
import im.vector.matrix.android.internal.session.room.state.SendStateTask
import im.vector.matrix.android.internal.session.room.timeline.*
import retrofit2.Retrofit

@Module
internal abstract class RoomModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesRoomAPI(retrofit: Retrofit): RoomAPI {
            return retrofit.create(RoomAPI::class.java)
        }
    }

    @Binds
    abstract fun bindRoomFactory(roomFactory: DefaultRoomFactory): RoomFactory

    @Binds
    abstract fun bindRoomService(roomService: DefaultRoomService): RoomService

    @Binds
    abstract fun bindRoomDirectoryService(roomDirectoryService: DefaultRoomDirectoryService): RoomDirectoryService

    @Binds
    abstract fun bindEventRelationsAggregationTask(eventRelationsAggregationTask: DefaultEventRelationsAggregationTask): EventRelationsAggregationTask

    @Binds
    abstract fun bindCreateRoomTask(createRoomTask: DefaultCreateRoomTask): CreateRoomTask

    @Binds
    abstract fun bindGetPublicRoomTask(getPublicRoomTask: DefaultGetPublicRoomTask): GetPublicRoomTask

    @Binds
    abstract fun bindGetThirdPartyProtocolsTask(getThirdPartyProtocolsTask: DefaultGetThirdPartyProtocolsTask): GetThirdPartyProtocolsTask

    @Binds
    abstract fun bindInviteTask(inviteTask: DefaultInviteTask): InviteTask

    @Binds
    abstract fun bindJoinRoomTask(joinRoomTask: DefaultJoinRoomTask): JoinRoomTask

    @Binds
    abstract fun bindLeaveRoomTask(leaveRoomTask: DefaultLeaveRoomTask): LeaveRoomTask

    @Binds
    abstract fun bindLoadRoomMembersTask(loadRoomMembersTask: DefaultLoadRoomMembersTask): LoadRoomMembersTask

    @Binds
    abstract fun bindPruneEventTask(pruneEventTask: DefaultPruneEventTask): PruneEventTask

    @Binds
    abstract fun bindSetReadMarkersTask(setReadMarkersTask: DefaultSetReadMarkersTask): SetReadMarkersTask

    @Binds
    abstract fun bindMarkAllRoomsReadTask(markAllRoomsReadTask: DefaultMarkAllRoomsReadTask): MarkAllRoomsReadTask

    @Binds
    abstract fun bindFindReactionEventForUndoTask(findReactionEventForUndoTask: DefaultFindReactionEventForUndoTask): FindReactionEventForUndoTask

    @Binds
    abstract fun bindUpdateQuickReactionTask(updateQuickReactionTask: DefaultUpdateQuickReactionTask): UpdateQuickReactionTask

    @Binds
    abstract fun bindSendStateTask(sendStateTask: DefaultSendStateTask): SendStateTask

    @Binds
    abstract fun bindGetContextOfEventTask(getContextOfEventTask: DefaultGetContextOfEventTask): GetContextOfEventTask

    @Binds
    abstract fun bindClearUnlinkedEventsTask(clearUnlinkedEventsTask: DefaultClearUnlinkedEventsTask): ClearUnlinkedEventsTask

    @Binds
    abstract fun bindPaginationTask(paginationTask: DefaultPaginationTask): PaginationTask

    @Binds
    abstract fun bindFileService(fileService: DefaultFileService): FileService

    @Binds
    abstract fun bindFetchEditHistoryTask(fetchEditHistoryTask: DefaultFetchEditHistoryTask): FetchEditHistoryTask
}
