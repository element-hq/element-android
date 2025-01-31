/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.login.ResetPasswordData
import org.matrix.android.sdk.internal.auth.registration.ThreePidData
import java.util.UUID

/**
 * This class holds all pending data when creating a session, either by login or by register.
 */
internal data class PendingSessionData(
        val homeServerConnectionConfig: HomeServerConnectionConfig,

        /* ==========================================================================================
         * Common
         * ========================================================================================== */

        val clientSecret: String = UUID.randomUUID().toString(),
        val sendAttempt: Int = 0,

        /* ==========================================================================================
         * For login
         * ========================================================================================== */

        val resetPasswordData: ResetPasswordData? = null,

        /* ==========================================================================================
         * For register
         * ========================================================================================== */

        val currentSession: String? = null,
        val isRegistrationStarted: Boolean = false,
        val currentThreePidData: ThreePidData? = null
)
