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

package im.vector.matrix.android.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.util.JSON_DICT_PARAMETERIZED_TYPE
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.model.UserAccountDataEntity
import im.vector.matrix.android.internal.database.model.UserAccountDataEntityFields
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataEvent
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import javax.inject.Inject

internal class DefaultAccountDataService @Inject constructor(
        private val monarchy: Monarchy,
        @SessionId private val sessionId: String,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val taskExecutor: TaskExecutor
) : AccountDataService {

    private val moshi = MoshiProvider.providesMoshi()
    private val adapter = moshi.adapter<Map<String, Any>>(JSON_DICT_PARAMETERIZED_TYPE)

    override fun getAccountData(type: String): UserAccountDataEvent? {
        return getAccountData(listOf(type)).firstOrNull()
    }

    override fun getLiveAccountData(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return Transformations.map(getLiveAccountData(listOf(type))) {
            it.firstOrNull()?.toOptional()
        }
    }

    override fun getAccountData(filterType: List<String>): List<UserAccountDataEvent> {
        return monarchy.fetchAllCopiedSync { realm ->
            realm.where(UserAccountDataEntity::class.java)
                    .apply {
                        if (filterType.isNotEmpty()) {
                            `in`(UserAccountDataEntityFields.TYPE, filterType.toTypedArray())
                        }
                    }
        }?.mapNotNull { entity ->
            entity.type?.let { type ->
                UserAccountDataEvent(
                        type = type,
                        content = entity.contentStr?.let { adapter.fromJson(it) } ?: emptyMap()
                )
            }
        } ?: emptyList()
    }

    override fun getLiveAccountData(filterType: List<String>): LiveData<List<UserAccountDataEvent>> {
        return monarchy.findAllMappedWithChanges({ realm ->
            realm.where(UserAccountDataEntity::class.java)
                    .apply {
                        if (filterType.isNotEmpty()) {
                            `in`(UserAccountDataEntityFields.TYPE, filterType.toTypedArray())
                        }
                    }
        }, { entity ->
            UserAccountDataEvent(
                    type = entity.type ?: "",
                    content = entity.contentStr?.let { adapter.fromJson(it) } ?: emptyMap()
            )
        })
    }

    override fun updateAccountData(type: String, data: Any, callback: MatrixCallback<Unit>?) {
        updateUserAccountDataTask.configureWith(UpdateUserAccountDataTask.AnyParams(
                type = type,
                any = data
        )) {
            this.retryCount = 5
            callback?.let { this.callback = it }
        }
                .executeBy(taskExecutor)
    }
}
