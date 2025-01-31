/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

internal open class UserThreePidEntity(
        var medium: String = "",
        var address: String = "",
        var validatedAt: Long = 0,
        var addedAt: Long = 0
) : RealmObject()
