/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.voicebroadcast.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://github.com/element-hq/element-meta/discussions/632
 */
@JsonClass(generateAdapter = false)
enum class VoiceBroadcastState(val value: String) {
    /**
     * The voice broadcast had been started and is currently being live.
     */
    @Json(name = "started") STARTED("started"),

    /**
     * The voice broadcast has been paused and may be resumed at any time by the recorder.
     */
    @Json(name = "paused") PAUSED("paused"),

    /**
     * The voice broadcast is currently being live again.
     */
    @Json(name = "resumed") RESUMED("resumed"),

    /**
     * The voice broadcast has ended.
     */
    @Json(name = "stopped") STOPPED("stopped"),
}
