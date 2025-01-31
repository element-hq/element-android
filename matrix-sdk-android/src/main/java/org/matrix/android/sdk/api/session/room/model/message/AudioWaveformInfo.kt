/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * See https://github.com/matrix-org/matrix-doc/blob/travis/msc/audio-waveform/proposals/3246-audio-waveform.md
 */
@JsonClass(generateAdapter = true)
data class AudioWaveformInfo(
        @Json(name = "duration")
        val duration: Int? = null,

        /**
         * The array should have no less than 30 elements and no more than 120.
         * List of integers between zero and 1024, inclusive.
         */
        @Json(name = "waveform")
        val waveform: List<Int?>? = null
)
