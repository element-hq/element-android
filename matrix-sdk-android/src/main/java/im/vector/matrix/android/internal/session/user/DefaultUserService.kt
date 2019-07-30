/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.model.UserEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.user.model.SearchUserTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchCopied
import javax.inject.Inject

internal class DefaultUserService @Inject constructor(private val monarchy: Monarchy,
                                                      private val searchUserTask: SearchUserTask,
                                                      private val taskExecutor: TaskExecutor) : UserService {

    private val realmDataSourceFactory: Monarchy.RealmDataSourceFactory<UserEntity> by lazy {
        monarchy.createDataSourceFactory { realm ->
            realm.where(UserEntity::class.java)
                    .isNotEmpty(UserEntityFields.USER_ID)
                    .sort(UserEntityFields.DISPLAY_NAME)
        }
    }

    private val domainDataSourceFactory: DataSource.Factory<Int, User> by lazy {
        realmDataSourceFactory.map {
            it.asDomain()
        }
    }

    private val livePagedListBuilder: LivePagedListBuilder<Int, User> by lazy {
        LivePagedListBuilder(domainDataSourceFactory, PagedList.Config.Builder().setPageSize(100).setEnablePlaceholders(false).build())
    }

    override fun getUser(userId: String): User? {
        val userEntity = monarchy.fetchCopied { UserEntity.where(it, userId).findFirst() }
                         ?: return null

        return userEntity.asDomain()
    }

    override fun liveUser(userId: String): LiveData<User?> {
        val liveRealmData = RealmLiveData(monarchy.realmConfiguration) { realm ->
            UserEntity.where(realm, userId)
        }
        return Transformations.map(liveRealmData) { results ->
            results
                    .map { it.asDomain() }
                    .firstOrNull()
        }
    }

    override fun liveUsers(): LiveData<List<User>> {
        return monarchy.findAllMappedWithChanges(
                { realm ->
                    realm.where(UserEntity::class.java)
                            .isNotEmpty(UserEntityFields.USER_ID)
                            .sort(UserEntityFields.DISPLAY_NAME)
                },
                { it.asDomain() }
        )
    }

    override fun livePagedUsers(filter: String?): LiveData<PagedList<User>> {
        realmDataSourceFactory.updateQuery { realm ->
            val query = realm.where(UserEntity::class.java)
            if (filter.isNullOrEmpty()) {
                query.isNotEmpty(UserEntityFields.USER_ID)
            } else {
                query
                        .beginGroup()
                        .contains(UserEntityFields.DISPLAY_NAME, filter)
                        .or()
                        .contains(UserEntityFields.USER_ID, filter)
                        .endGroup()
            }
            query.sort(UserEntityFields.DISPLAY_NAME)
        }
        return monarchy.findAllPagedWithChanges(realmDataSourceFactory, livePagedListBuilder)
    }


    override fun searchUsersDirectory(search: String,
                                      limit: Int,
                                      excludedUserIds: Set<String>,
                                      callback: MatrixCallback<List<User>>): Cancelable {
        val params = SearchUserTask.Params(limit, search, excludedUserIds)
        return searchUserTask
                .configureWith(params)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }
}
