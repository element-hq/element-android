/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import io.realm.Realm
import java.io.Closeable

internal class RealmInstanceWrapper(private val realm: Realm, private val closeRealmOnClose: Boolean) : Closeable {

    override fun close() {
        if (closeRealmOnClose) {
            realm.close()
        }
    }

    fun <R> withRealm(block: (Realm) -> R): R {
        return use {
            block(it.realm)
        }
    }
}
