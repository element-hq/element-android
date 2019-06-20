/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.fetchCopied
import javax.inject.Inject

internal class DefaultUserService @Inject constructor(private val monarchy: Monarchy) : UserService {

    override fun getUser(userId: String): User? {
        val userEntity = monarchy.fetchCopied { UserEntity.where(it, userId).findFirst() }
                         ?: return null

        return userEntity.asDomain()
    }

    override fun observeUser(userId: String): LiveData<User?> {
        val liveRealmData = RealmLiveData(monarchy.realmConfiguration) { realm ->
            UserEntity.where(realm, userId)
        }
        return Transformations.map(liveRealmData) { results ->
            results
                    .map { it.asDomain() }
                    .firstOrNull()
        }
    }
}