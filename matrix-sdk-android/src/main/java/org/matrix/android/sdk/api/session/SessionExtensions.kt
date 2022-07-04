/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session

import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.user.model.User

/**
 * Get a room using the RoomService of a Session.
 */
fun Session.getRoom(roomId: String): Room? = roomService().getRoom(roomId)

/**
 * Get a room summary using the RoomService of a Session.
 */
fun Session.getRoomSummary(roomIdOrAlias: String): RoomSummary? = roomService().getRoomSummary(roomIdOrAlias)

/**
 * Get a user using the UserService of a Session.
 */
fun Session.getUser(userId: String): User? = userService().getUser(userId)
