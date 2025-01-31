/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

/**
 * Interface representing an room key action request.
 * Note: this class cannot be abstract because of [org.matrix.androidsdk.core.JsonUtils.toRoomKeyShare]
 */
interface GossipingToDeviceObject : SendToDeviceObject {

    val action: String?

    val requestingDeviceId: String?

    val requestId: String?

    companion object {
        const val ACTION_SHARE_REQUEST = "request"
        const val ACTION_SHARE_CANCELLATION = "request_cancellation"
    }
}
