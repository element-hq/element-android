/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.call

interface CallSignalingContent {
    /**
     * Required. A unique identifier for the call.
     */
    val callId: String?

    /**
     * Required. ID to let user identify remote echo of their own events
     */
    val partyId: String?

    /**
     * Required. The version of the VoIP specification this message adheres to. This specification is version 0.
     */
    val version: String?
}
