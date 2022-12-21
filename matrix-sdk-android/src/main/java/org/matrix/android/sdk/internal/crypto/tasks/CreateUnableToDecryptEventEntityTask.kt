/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.tasks

import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.crypto.store.db.doRealmTransactionAsync
import org.matrix.android.sdk.internal.database.model.UnableToDecryptEventEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

/**
 * This task create a dedicated entity for UTD events so that it can be processed later.
 */
internal interface CreateUnableToDecryptEventEntityTask : Task<CreateUnableToDecryptEventEntityTask.Params, Unit> {
    data class Params(
            val eventId: String,
    )
}

// TODO add unit tests
internal class DefaultCreateUnableToDecryptEventEntityTask @Inject constructor(
        @SessionDatabase val realmConfiguration: RealmConfiguration,
) : CreateUnableToDecryptEventEntityTask {

    override suspend fun execute(params: CreateUnableToDecryptEventEntityTask.Params) {
        val utdEventEntity = UnableToDecryptEventEntity(eventId = params.eventId)
        doRealmTransactionAsync(realmConfiguration) { realm ->
            realm.insert(utdEventEntity)
        }
    }
}
