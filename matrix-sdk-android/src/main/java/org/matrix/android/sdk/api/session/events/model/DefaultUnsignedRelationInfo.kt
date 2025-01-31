/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DefaultUnsignedRelationInfo(
        override val limited: Boolean? = false,
        override val count: Int? = 0,
        val chunk: List<Map<String, Any>>? = null

) : UnsignedRelationInfo
