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

import im.vector.matrix.android.internal.di.IdentityDatabase
import im.vector.matrix.android.internal.session.identity.model.IdentityHashDetailResponse
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class RealmIdentityServerStore @Inject constructor(
        @IdentityDatabase
        private val realmConfiguration: RealmConfiguration
) : IdentityServiceStore {

    override fun get(): IdentityServerEntity {
        return Realm.getInstance(realmConfiguration).use {
            IdentityServerEntity.getOrCreate(it)
        }
    }

    override fun setUrl(url: String?) {
        Realm.getInstance(realmConfiguration).use {
            IdentityServerEntity.setUrl(it, url)
        }
    }

    override fun setToken(token: String?) {
        Realm.getInstance(realmConfiguration).use {
            IdentityServerEntity.setToken(it, token)
        }
    }

    override fun setHashDetails(hashDetailResponse: IdentityHashDetailResponse) {
        Realm.getInstance(realmConfiguration).use {
            IdentityServerEntity.setHashDetails(it, hashDetailResponse.pepper, hashDetailResponse.algorithms)
        }
    }
}
