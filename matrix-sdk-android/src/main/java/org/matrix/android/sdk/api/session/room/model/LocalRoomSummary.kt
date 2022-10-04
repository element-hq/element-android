/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

/**
 * This class holds some data of a local room.
 * It can be retrieved by [org.matrix.android.sdk.api.session.room.Room] and [org.matrix.android.sdk.api.session.room.RoomService]
 */
data class LocalRoomSummary(
        /**
         * The roomId of the room.
         */
        val roomId: String,
        /**
         * The room summary of the room.
         */
        val roomSummary: RoomSummary?,
        /**
         * The creation params attached to the room.
         */
        val createRoomParams: CreateRoomParams?,
        /**
         * The roomId of the created room (ie. created on the server), if any.
         */
        val replacementRoomId: String?,
        /**
         * The creation state of the room.
         */
        val creationState: LocalRoomCreationState,
)
