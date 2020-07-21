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
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.session.sync.UserAccountDataSyncHandler
import im.vector.matrix.android.api.session.accountdata.UserAccountDataEvent
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import javax.inject.Inject

internal class DefaultAccountDataService @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
        private val accountDataDataSource: AccountDataDataSource,
        private val taskExecutor: TaskExecutor
) : AccountDataService {

    override fun getAccountDataEvent(type: String): UserAccountDataEvent? {
        return accountDataDataSource.getAccountDataEvent(type)
    }

    override fun getLiveAccountDataEvent(type: String): LiveData<Optional<UserAccountDataEvent>> {
        return accountDataDataSource.getLiveAccountDataEvent(type)
    }

    override fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        return accountDataDataSource.getAccountDataEvents(types)
    }

    override fun getLiveAccountDataEvents(types: Set<String>): LiveData<List<UserAccountDataEvent>> {
        return accountDataDataSource.getLiveAccountDataEvents(types)
    }

    override fun updateAccountData(type: String, content: Content, callback: MatrixCallback<Unit>?): Cancelable {
        return updateUserAccountDataTask.configureWith(UpdateUserAccountDataTask.AnyParams(
                type = type,
                any = content
        )) {
            this.retryCount = 5
            this.callback = object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    // TODO Move that to the task (but it created a circular dependencies...)
                    monarchy.runTransactionSync { realm ->
                        userAccountDataSyncHandler.handleGenericAccountData(realm, type, content)
                    }
                    callback?.onSuccess(data)
                }

                override fun onFailure(failure: Throwable) {
                    callback?.onFailure(failure)
                }
            }
        }
                .executeBy(taskExecutor)
    }
}
