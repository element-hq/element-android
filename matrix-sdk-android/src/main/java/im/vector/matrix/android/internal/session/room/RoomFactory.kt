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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.database.mapper.RoomSummaryMapper
import im.vector.matrix.android.internal.session.room.draft.DefaultDraftService
import im.vector.matrix.android.internal.session.room.membership.DefaultMembershipService
import im.vector.matrix.android.internal.session.room.read.DefaultReadService
import im.vector.matrix.android.internal.session.room.relation.DefaultRelationService
import im.vector.matrix.android.internal.session.room.send.DefaultSendService
import im.vector.matrix.android.internal.session.room.state.DefaultStateService
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineService
import javax.inject.Inject


internal interface RoomFactory {
    fun create(roomId: String): Room
}

internal class DefaultRoomFactory @Inject constructor(private val monarchy: Monarchy,
                                                      private val roomSummaryMapper: RoomSummaryMapper,
                                                      private val cryptoService: CryptoService,
                                                      private val timelineServiceFactory: DefaultTimelineService.Factory,
                                                      private val sendServiceFactory: DefaultSendService.Factory,
                                                      private val draftServiceFactory: DefaultDraftService.Factory,
                                                      private val stateServiceFactory: DefaultStateService.Factory,
                                                      private val readServiceFactory: DefaultReadService.Factory,
                                                      private val relationServiceFactory: DefaultRelationService.Factory,
                                                      private val membershipServiceFactory: DefaultMembershipService.Factory) :
        RoomFactory {

    override fun create(roomId: String): Room {
        return DefaultRoom(
                roomId,
                monarchy,
                roomSummaryMapper,
                timelineServiceFactory.create(roomId),
                sendServiceFactory.create(roomId),
                draftServiceFactory.create(roomId),
                stateServiceFactory.create(roomId),
                readServiceFactory.create(roomId),
                cryptoService,
                relationServiceFactory.create(roomId),
                membershipServiceFactory.create(roomId)
        )
    }

}