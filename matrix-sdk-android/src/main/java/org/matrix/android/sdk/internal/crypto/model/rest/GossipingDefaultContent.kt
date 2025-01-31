/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject

@JsonClass(generateAdapter = true)
internal data class GossipingDefaultContent(
        @Json(name = "action") override val action: String?,
        @Json(name = "requesting_device_id") override val requestingDeviceId: String?,
        @Json(name = "m.request_id") override val requestId: String? = null
) : GossipingToDeviceObject
