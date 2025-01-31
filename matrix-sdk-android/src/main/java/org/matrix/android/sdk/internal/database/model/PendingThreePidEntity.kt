/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

/**
 * This class is used to store pending threePid data, when user wants to add a threePid to his account.
 */
internal open class PendingThreePidEntity(
        var email: String? = null,
        var msisdn: String? = null,
        var clientSecret: String = "",
        var sendAttempt: Int = 0,
        var sid: String = "",
        var submitUrl: String? = null
) : RealmObject()
