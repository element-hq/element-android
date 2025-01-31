/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.createObject
import io.realm.kotlin.where

/**
 * Only one object can be stored at a time.
 */
internal fun IdentityDataEntity.Companion.get(realm: Realm): IdentityDataEntity? {
    return realm.where<IdentityDataEntity>().findFirst()
}

private fun IdentityDataEntity.Companion.getOrCreate(realm: Realm): IdentityDataEntity {
    return get(realm) ?: realm.createObject()
}

internal fun IdentityDataEntity.Companion.setUrl(
        realm: Realm,
        url: String?
) {
    realm.where<IdentityDataEntity>().findAll().deleteAllFromRealm()
    // Delete all pending binding if any
    IdentityPendingBindingEntity.deleteAll(realm)

    if (url != null) {
        getOrCreate(realm).apply {
            identityServerUrl = url
        }
    }
}

internal fun IdentityDataEntity.Companion.setToken(
        realm: Realm,
        newToken: String?
) {
    get(realm)?.apply {
        token = newToken
    }
}

internal fun IdentityDataEntity.Companion.setUserConsent(
        realm: Realm,
        newConsent: Boolean
) {
    get(realm)?.apply {
        userConsent = newConsent
    }
}

internal fun IdentityDataEntity.Companion.setHashDetails(
        realm: Realm,
        pepper: String,
        algorithms: List<String>
) {
    get(realm)?.apply {
        hashLookupPepper = pepper
        hashLookupAlgorithm = RealmList<String>().apply { addAll(algorithms) }
    }
}
