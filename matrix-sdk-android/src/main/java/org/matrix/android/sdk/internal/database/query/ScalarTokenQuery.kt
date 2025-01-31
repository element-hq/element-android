/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.ScalarTokenEntity
import org.matrix.android.sdk.internal.database.model.ScalarTokenEntityFields

internal fun ScalarTokenEntity.Companion.where(realm: Realm, serverUrl: String): RealmQuery<ScalarTokenEntity> {
    return realm
            .where<ScalarTokenEntity>()
            .equalTo(ScalarTokenEntityFields.SERVER_URL, serverUrl)
}
