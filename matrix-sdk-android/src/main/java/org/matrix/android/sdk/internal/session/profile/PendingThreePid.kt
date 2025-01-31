/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.session.identity.ThreePid

internal data class PendingThreePid(
        val threePid: ThreePid,
        val clientSecret: String,
        val sendAttempt: Int,
        // For Msisdn and Email
        val sid: String,
        // For Msisdn only
        val submitUrl: String?
)
