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
import im.vector.matrix.android.internal.session.room.alias.AddRoomAliasTask
import im.vector.matrix.android.internal.session.room.alias.DefaultAddRoomAliasTask
import im.vector.matrix.android.internal.session.room.alias.DefaultGetRoomIdByAliasTask
import im.vector.matrix.android.internal.session.room.alias.GetRoomIdByAliasTask
import im.vector.matrix.android.internal.session.room.create.CreateRoomTask
import im.vector.matrix.android.internal.session.room.create.DefaultCreateRoomTask
import im.vector.matrix.android.internal.session.room.directory.DefaultGetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.DefaultGetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.directory.GetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.GetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.membership.DefaultLoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.membership.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.membership.admin.DefaultMembershipAdminTask
import im.vector.matrix.android.internal.session.room.membership.admin.MembershipAdminTask
import im.vector.matrix.android.internal.session.room.membership.joining.DefaultInviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.DefaultJoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.joining.InviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.DefaultLeaveRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.LeaveRoomTask
import im.vector.matrix.android.internal.session.room.membership.threepid.DefaultInviteThreePidTask
import im.vector.matrix.android.internal.session.room.membership.threepid.InviteThreePidTask
import im.vector.matrix.android.internal.session.room.read.DefaultMarkAllRoomsReadTask
import im.vector.matrix.android.internal.session.room.read.DefaultSetReadMarkersTask
import im.vector.matrix.android.internal.session.room.read.MarkAllRoomsReadTask
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.session.room.relation.DefaultFetchEditHistoryTask
import im.vector.matrix.android.internal.session.room.relation.DefaultFindReactionEventForUndoTask
import im.vector.matrix.android.internal.session.room.relation.DefaultUpdateQuickReactionTask
import im.vector.matrix.android.internal.session.room.relation.FetchEditHistoryTask
import im.vector.matrix.android.internal.session.room.relation.FindReactionEventForUndoTask
import im.vector.matrix.android.internal.session.room.relation.UpdateQuickReactionTask
import im.vector.matrix.android.internal.session.room.reporting.DefaultReportContentTask
import im.vector.matrix.android.internal.session.room.reporting.ReportContentTask
import im.vector.matrix.android.internal.session.room.state.DefaultSendStateTask
import im.vector.matrix.android.internal.session.room.state.SendStateTask
import im.vector.matrix.android.internal.session.room.tags.AddTagToRoomTask
import im.vector.matrix.android.internal.session.room.tags.DefaultAddTagToRoomTask
import im.vector.matrix.android.internal.session.room.tags.DefaultDeleteTagFromRoomTask
import im.vector.matrix.android.internal.session.room.tags.DeleteTagFromRoomTask
import im.vector.matrix.android.internal.session.room.timeline.DefaultFetchTokenAndPaginateTask
import im.vector.matrix.android.internal.session.room.timeline.DefaultGetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.DefaultPaginationTask
import im.vector.matrix.android.internal.session.room.timeline.FetchTokenAndPaginateTask
import im.vector.matrix.android.internal.session.room.timeline.GetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.PaginationTask
import im.vector.matrix.android.internal.session.room.typing.DefaultSendTypingTask
import im.vector.matrix.android.internal.session.room.typing.SendTypingTask
import im.vector.matrix.android.internal.session.room.uploads.DefaultGetUploadsTask
import im.vector.matrix.android.internal.session.room.uploads.GetUploadsTask
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
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

        @Provides
        @JvmStatic
        fun providesParser(): Parser {
            return Parser.builder().build()
        }

        @Provides
        @JvmStatic
        fun providesHtmlRenderer(): HtmlRenderer {
            return HtmlRenderer
                    .builder()
                    .build()
        }

        @Provides
        @JvmStatic
        fun providesTextContentRenderer(): TextContentRenderer {
            return TextContentRenderer
                    .builder()
                    .build()
        }
    }

    @Binds
    abstract fun bindRoomFactory(factory: DefaultRoomFactory): RoomFactory

    @Binds
    abstract fun bindRoomGetter(getter: DefaultRoomGetter): RoomGetter

    @Binds
    abstract fun bindRoomService(service: DefaultRoomService): RoomService

    @Binds
    abstract fun bindRoomDirectoryService(service: DefaultRoomDirectoryService): RoomDirectoryService

    @Binds
    abstract fun bindFileService(service: DefaultFileService): FileService

    @Binds
    abstract fun bindCreateRoomTask(task: DefaultCreateRoomTask): CreateRoomTask

    @Binds
    abstract fun bindGetPublicRoomTask(task: DefaultGetPublicRoomTask): GetPublicRoomTask

    @Binds
    abstract fun bindGetThirdPartyProtocolsTask(task: DefaultGetThirdPartyProtocolsTask): GetThirdPartyProtocolsTask

    @Binds
    abstract fun bindInviteTask(task: DefaultInviteTask): InviteTask

    @Binds
    abstract fun bindInviteThreePidTask(task: DefaultInviteThreePidTask): InviteThreePidTask

    @Binds
    abstract fun bindJoinRoomTask(task: DefaultJoinRoomTask): JoinRoomTask

    @Binds
    abstract fun bindLeaveRoomTask(task: DefaultLeaveRoomTask): LeaveRoomTask

    @Binds
    abstract fun bindMembershipAdminTask(task: DefaultMembershipAdminTask): MembershipAdminTask

    @Binds
    abstract fun bindLoadRoomMembersTask(task: DefaultLoadRoomMembersTask): LoadRoomMembersTask

    @Binds
    abstract fun bindSetReadMarkersTask(task: DefaultSetReadMarkersTask): SetReadMarkersTask

    @Binds
    abstract fun bindMarkAllRoomsReadTask(task: DefaultMarkAllRoomsReadTask): MarkAllRoomsReadTask

    @Binds
    abstract fun bindFindReactionEventForUndoTask(task: DefaultFindReactionEventForUndoTask): FindReactionEventForUndoTask

    @Binds
    abstract fun bindUpdateQuickReactionTask(task: DefaultUpdateQuickReactionTask): UpdateQuickReactionTask

    @Binds
    abstract fun bindSendStateTask(task: DefaultSendStateTask): SendStateTask

    @Binds
    abstract fun bindReportContentTask(task: DefaultReportContentTask): ReportContentTask

    @Binds
    abstract fun bindGetContextOfEventTask(task: DefaultGetContextOfEventTask): GetContextOfEventTask

    @Binds
    abstract fun bindPaginationTask(task: DefaultPaginationTask): PaginationTask

    @Binds
    abstract fun bindFetchNextTokenAndPaginateTask(task: DefaultFetchTokenAndPaginateTask): FetchTokenAndPaginateTask

    @Binds
    abstract fun bindFetchEditHistoryTask(task: DefaultFetchEditHistoryTask): FetchEditHistoryTask

    @Binds
    abstract fun bindGetRoomIdByAliasTask(task: DefaultGetRoomIdByAliasTask): GetRoomIdByAliasTask

    @Binds
    abstract fun bindAddRoomAliasTask(task: DefaultAddRoomAliasTask): AddRoomAliasTask

    @Binds
    abstract fun bindSendTypingTask(task: DefaultSendTypingTask): SendTypingTask

    @Binds
    abstract fun bindGetUploadsTask(task: DefaultGetUploadsTask): GetUploadsTask

    @Binds
    abstract fun bindAddTagToRoomTask(task: DefaultAddTagToRoomTask): AddTagToRoomTask

    @Binds
    abstract fun bindDeleteTagFromRoomTask(task: DefaultDeleteTagFromRoomTask): DeleteTagFromRoomTask
}
