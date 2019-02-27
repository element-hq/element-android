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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.fetchCopied

internal class DefaultUserService(private val monarchy: Monarchy) : UserService {

    override fun getUser(userId: String): User? {
        val userEntity = monarchy.fetchCopied { UserEntity.where(it, userId).findFirst() }
                ?: return null

        return User(
                userEntity.userId,
                userEntity.displayName,
                userEntity.avatarUrl
        )
    }
}