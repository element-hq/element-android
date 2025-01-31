/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.membership.threepid

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.auth.data.ThreePidMedium

@JsonClass(generateAdapter = true)
internal data class ThreePidInviteBody(
        /**
         * Required. The hostname+port of the identity server which should be used for third party identifier lookups.
         */
        @Json(name = "id_server")
        val idServer: String,
        /**
         * Required. An access token previously registered with the identity server. Servers can treat this as optional
         * to distinguish between r0.5-compatible clients and this specification version.
         */
        @Json(name = "id_access_token")
        val idAccessToken: String,
        /**
         * Required. The kind of address being passed in the address field, for example email.
         */
        @Json(name = "medium")
        val medium: String,
        /**
         * Required. The invitee's third party identifier.
         */
        @Json(name = "address")
        val address: String
)

internal fun ThreePidInviteBody.toThreePid() = when (medium) {
    ThreePidMedium.EMAIL -> ThreePid.Email(address)
    ThreePidMedium.MSISDN -> ThreePid.Msisdn(address)
    else -> null
}
