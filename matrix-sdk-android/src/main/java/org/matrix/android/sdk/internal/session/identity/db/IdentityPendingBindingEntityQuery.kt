/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.identity.ThreePid

internal fun IdentityPendingBindingEntity.Companion.get(realm: Realm, threePid: ThreePid): IdentityPendingBindingEntity? {
    return realm.where<IdentityPendingBindingEntity>()
            .equalTo(IdentityPendingBindingEntityFields.THREE_PID, threePid.toPrimaryKey())
            .findFirst()
}

internal fun IdentityPendingBindingEntity.Companion.getOrCreate(realm: Realm, threePid: ThreePid): IdentityPendingBindingEntity {
    return get(realm, threePid) ?: realm.createObject(threePid.toPrimaryKey())
}

internal fun IdentityPendingBindingEntity.Companion.delete(realm: Realm, threePid: ThreePid) {
    get(realm, threePid)?.deleteFromRealm()
}

internal fun IdentityPendingBindingEntity.Companion.deleteAll(realm: Realm) {
    realm.where<IdentityPendingBindingEntity>()
            .findAll()
            .deleteAllFromRealm()
}
