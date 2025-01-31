/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import org.matrix.android.sdk.api.session.events.model.Content

data class MXEncryptEventContentResult(
        /**
         * The encrypted event content.
         */
        val eventContent: Content,
        /**
         * The event type.
         */
        val eventType: String
)
