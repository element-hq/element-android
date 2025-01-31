/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.session.identity.db.IdentityDataEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateIdentityTo001(realm: DynamicRealm) : RealmMigrator(realm, 1) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Add field userConsent (Boolean) and set the value to false")
        realm.schema.get("IdentityDataEntity")
                ?.addField(IdentityDataEntityFields.USER_CONSENT, Boolean::class.java)
    }
}
