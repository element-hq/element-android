/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
