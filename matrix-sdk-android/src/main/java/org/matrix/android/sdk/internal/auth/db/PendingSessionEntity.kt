/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import io.realm.RealmObject

internal open class PendingSessionEntity(
        var homeServerConnectionConfigJson: String = "",
        var clientSecret: String = "",
        var sendAttempt: Int = 0,
        var resetPasswordDataJson: String? = null,
        var currentSession: String? = null,
        var isRegistrationStarted: Boolean = false,
        var currentThreePidDataJson: String? = null
) : RealmObject()
