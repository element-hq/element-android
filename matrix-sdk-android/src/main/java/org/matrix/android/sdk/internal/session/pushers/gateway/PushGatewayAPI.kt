/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers.gateway

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.POST

internal interface PushGatewayAPI {
    /**
     * Ask the Push Gateway to send a push to the current device.
     *
     * Ref: https://matrix.org/docs/spec/push_gateway/r0.1.1#post-matrix-push-v1-notify
     */
    @POST(NetworkConstants.URI_PUSH_GATEWAY_PREFIX_PATH + "notify")
    suspend fun notify(@Body body: PushGatewayNotifyBody): PushGatewayNotifyResponse
}
