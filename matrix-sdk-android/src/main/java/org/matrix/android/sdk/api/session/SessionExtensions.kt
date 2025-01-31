/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
 * @param userId the userId to look for.
 * @return a user with userId or null if the User is not known yet by the SDK.
 * See [org.matrix.android.sdk.api.session.user.UserService.resolveUser] to ensure that a User is retrieved.
 */
fun Session.getUser(userId: String): User? = userService().getUser(userId)

/**
 * Similar to [getUser], but fallback to a User without details if the User is not known by the SDK, or if Session is null.
 */
fun Session?.getUserOrDefault(userId: String): User = this?.userService()?.getUser(userId) ?: User(userId)
