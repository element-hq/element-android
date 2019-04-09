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
import im.vector.matrix.android.internal.session.room.invite.DefaultInviteTask
import im.vector.matrix.android.internal.session.room.invite.InviteTask
import im.vector.matrix.android.internal.session.room.members.DefaultLoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.read.DefaultSetReadMarkersTask
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.session.room.send.EventFactory
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
            DefaultPaginationTask(get(), get()) as PaginationTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetContextOfEventTask(get(), get()) as GetContextOfEventTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultSetReadMarkersTask(get(), get(), get()) as SetReadMarkersTask
        }

        scope(DefaultSession.SCOPE) {
            EventFactory(get())
        }

        scope(DefaultSession.SCOPE) {
            RoomFactory(get(), get(), get(), get(), get(), get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultCreateRoomTask(get(), get()) as CreateRoomTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultInviteTask(get()) as InviteTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultSendStateTask(get()) as SendStateTask
        }

    }
}
