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

import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.room.create.CreateRoomTask
import im.vector.matrix.android.internal.session.room.create.DefaultCreateRoomTask
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
import im.vector.matrix.android.internal.session.room.read.DefaultSetReadMarkersTask
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.session.room.relation.DefaultFindReactionEventForUndoTask
import im.vector.matrix.android.internal.session.room.relation.FindReactionEventForUndoTask
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.LocalEchoUpdater
import im.vector.matrix.android.internal.session.room.state.DefaultSendStateTask
import im.vector.matrix.android.internal.session.room.state.SendStateTask
import im.vector.matrix.android.internal.session.room.timeline.*
import org.koin.dsl.module.module
import retrofit2.Retrofit


class RoomModule {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(RoomAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            DefaultLoadRoomMembersTask(get(), get(), get(), get()) as LoadRoomMembersTask
        }

        scope(DefaultSession.SCOPE) {
            TokenChunkEventPersistor(get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultPaginationTask(get(), get(), get()) as PaginationTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetContextOfEventTask(get(), get(), get()) as GetContextOfEventTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultSetReadMarkersTask(get(), get(), get()) as SetReadMarkersTask
        }

        scope(DefaultSession.SCOPE) {
            LocalEchoEventFactory(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            LocalEchoUpdater(get())
        }

        scope(DefaultSession.SCOPE) {
            RoomFactory(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultCreateRoomTask(get(), get("SessionRealmConfiguration")) as CreateRoomTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultInviteTask(get()) as InviteTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultJoinRoomTask(get()) as JoinRoomTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultLeaveRoomTask(get()) as LeaveRoomTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultSendStateTask(get()) as SendStateTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultFindReactionEventForUndoTask(get()) as FindReactionEventForUndoTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultPruneEventTask(get()) as PruneEventTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultEventRelationsAggregationTask(get()) as EventRelationsAggregationTask
        }

    }
}
