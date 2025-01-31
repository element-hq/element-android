/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class EndCallReason {
    @Json(name = "ice_failed")
    ICE_FAILED,

    @Json(name = "ice_timeout")
    ICE_TIMEOUT,

    @Json(name = "user_hangup")
    USER_HANGUP,

    @Json(name = "replaced")
    REPLACED,

    @Json(name = "user_media_failed")
    USER_MEDIA_FAILED,

    @Json(name = "invite_timeout")
    INVITE_TIMEOUT,

    @Json(name = "unknown_error")
    UNKWOWN_ERROR,

    @Json(name = "user_busy")
    USER_BUSY,

    @Json(name = "answered_elsewhere")
    ANSWERED_ELSEWHERE
}
