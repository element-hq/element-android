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
package im.vector.matrix.android.internal.crypto.verification

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.internal.crypto.tasks.DefaultRoomVerificationUpdateTask
import im.vector.matrix.android.internal.crypto.tasks.RoomVerificationUpdateTask
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.types
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmConfiguration
import io.realm.RealmResults
import javax.inject.Inject

internal class VerificationMessageLiveObserver @Inject constructor(
        @SessionDatabase realmConfiguration: RealmConfiguration,
        private val roomVerificationUpdateTask: DefaultRoomVerificationUpdateTask,
        private val cryptoService: CryptoService,
        private val sasVerificationService: DefaultSasVerificationService,
        private val taskExecutor: TaskExecutor
) : RealmLiveEntityObserver<EventEntity>(realmConfiguration) {

    override val query = Monarchy.Query {
        EventEntity.types(it, listOf(
                EventType.KEY_VERIFICATION_START,
                EventType.KEY_VERIFICATION_ACCEPT,
                EventType.KEY_VERIFICATION_KEY,
                EventType.KEY_VERIFICATION_MAC,
                EventType.KEY_VERIFICATION_CANCEL,
                EventType.KEY_VERIFICATION_DONE,
                EventType.MESSAGE,
                EventType.ENCRYPTED)
        )
    }

    override fun onChange(results: RealmResults<EventEntity>, changeSet: OrderedCollectionChangeSet) {
        // Should we ignore when it's an initial sync?
        val events = changeSet.insertions
                .asSequence()
                .mapNotNull { results[it]?.asDomain() }
                .filterNot {
                    // ignore local echos
                    LocalEcho.isLocalEchoId(it.eventId ?: "")
                }
                .toList()

        roomVerificationUpdateTask.configureWith(
                RoomVerificationUpdateTask.Params(events, sasVerificationService, cryptoService)
        ).executeBy(taskExecutor)
    }
}
