/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.identity.db

import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.createObject
import io.realm.kotlin.where

/**
 * Only one object can be stored at a time
 */
internal fun IdentityServerEntity.Companion.get(realm: Realm): IdentityServerEntity? {
    return realm.where<IdentityServerEntity>().findFirst()
}

private fun IdentityServerEntity.Companion.getOrCreate(realm: Realm): IdentityServerEntity {
    return get(realm) ?: realm.createObject()
}

internal fun IdentityServerEntity.Companion.setUrl(realm: Realm,
                                                   url: String?) {
    realm.where<IdentityServerEntity>().findAll().deleteAllFromRealm()

    if (url != null) {
        getOrCreate(realm).apply {
            identityServerUrl = url
        }
    }
}

internal fun IdentityServerEntity.Companion.setToken(realm: Realm,
                                                     newToken: String?) {
    getOrCreate(realm).apply {
        token = newToken
    }
}

internal fun IdentityServerEntity.Companion.setHashDetails(realm: Realm,
                                                           pepper: String,
                                                           algorithms: List<String>) {
    getOrCreate(realm).apply {
        hashLookupPepper = pepper
        hashLookupAlgorithm = RealmList<String>().apply { addAll(algorithms) }
    }
}
