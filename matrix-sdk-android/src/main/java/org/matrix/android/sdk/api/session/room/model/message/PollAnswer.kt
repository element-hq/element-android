/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PollAnswer(
        @Json(name = "id") val id: String? = null,
        @Json(name = "org.matrix.msc1767.text") val unstableAnswer: String? = null,
        @Json(name = "m.text") val answer: String? = null
) {

    fun getBestAnswer() = answer ?: unstableAnswer
}
