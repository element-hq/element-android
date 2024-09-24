/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.data

sealed class PollHistoryError : Exception() {
    object UnknownRoomError : PollHistoryError()
}
