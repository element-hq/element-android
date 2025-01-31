/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class SessionParamsEntity(
        @PrimaryKey var sessionId: String = "",
        var userId: String = "",
        var credentialsJson: String = "",
        var homeServerConnectionConfigJson: String = "",
        // Set to false when the token is invalid and the user has been soft logged out
        // In case of hard logout, this object is deleted from DB
        var isTokenValid: Boolean = true,
        var loginType: String = "",
) : RealmObject()
