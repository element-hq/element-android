/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.user.accountdata

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.accountdata.AccountDataService
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.UserAccountDataSyncHandler
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
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
