/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SendToDeviceBody(
        /**
         * `Any` should implement [SendToDeviceObject], but we cannot use interface here because of Json serialization
         *
         * The messages to send. A map from user ID, to a map from device ID to message body.
         * The device ID may also be *, meaning all known devices for the user.
         */
        val messages: Map<String, Map<String, Any>>?
)
