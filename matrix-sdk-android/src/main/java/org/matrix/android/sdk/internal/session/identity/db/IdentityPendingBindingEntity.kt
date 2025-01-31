/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium

internal open class IdentityPendingBindingEntity(
        @PrimaryKey var threePid: String = "",
        /* Managed by Riot */
        var clientSecret: String = "",
        /* Managed by Riot */
        var sendAttempt: Int = 0,
        /* Provided by the identity server */
        var sid: String = ""
) : RealmObject() {

    companion object {
        fun ThreePid.toPrimaryKey() = "${toMedium()}_$value"
    }
}
