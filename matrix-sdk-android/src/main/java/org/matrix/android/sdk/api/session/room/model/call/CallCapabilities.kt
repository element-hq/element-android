/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.orFalse

@JsonClass(generateAdapter = true)
data class CallCapabilities(
        /**
         * If set to true, states that the sender of the event supports the m.call.replaces event and therefore supports
         * being transferred to another destination.
         */
        @Json(name = "m.call.transferee") val transferee: Boolean? = null
)

fun CallCapabilities?.supportCallTransfer() = this?.transferee.orFalse()
