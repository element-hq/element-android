/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.crosssigning

import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.CryptoDatabase
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.createBackgroundHandler
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

internal class ShieldTrustUpdater @Inject constructor(
        private val eventBus: EventBus,
        private val computeTrustTask: ComputeTrustTask,
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        @CryptoDatabase private val cryptoRealmConfiguration: RealmConfiguration,
        @SessionDatabase private val sessionRealmConfiguration: RealmConfiguration,
        private val roomSummaryUpdater: RoomSummaryUpdater
) {

    companion object {
        private val BACKGROUND_HANDLER = createBackgroundHandler("SHIELD_CRYPTO_DB_THREAD")
    }

    private val backgroundCryptoRealm = AtomicReference<Realm>()
    private val backgroundSessionRealm = AtomicReference<Realm>()

//    private var cryptoDevicesResult: RealmResults<DeviceInfoEntity>? = null

//    private val cryptoDeviceChangeListener = object : OrderedRealmCollectionChangeListener<RealmResults<DeviceInfoEntity>> {
//        override fun onChange(t: RealmResults<DeviceInfoEntity>, changeSet: OrderedCollectionChangeSet) {
//            val grouped = t.groupBy { it.userId }
//            onCryptoDevicesChange(grouped.keys.mapNotNull { it })
//        }
//    }

    fun start() {
        eventBus.register(this)
        BACKGROUND_HANDLER.post {
            val cryptoRealm = Realm.getInstance(cryptoRealmConfiguration)
            backgroundCryptoRealm.set(cryptoRealm)
//            cryptoDevicesResult = cryptoRealm.where<DeviceInfoEntity>().findAll()
//            cryptoDevicesResult?.addChangeListener(cryptoDeviceChangeListener)

            backgroundSessionRealm.set(Realm.getInstance(sessionRealmConfiguration))
        }
    }

    fun stop() {
        eventBus.unregister(this)
        BACKGROUND_HANDLER.post {
            //            cryptoDevicesResult?.removeAllChangeListeners()
            backgroundCryptoRealm.getAndSet(null).also {
                it?.close()
            }
            backgroundSessionRealm.getAndSet(null).also {
                it?.close()
            }
        }
    }

    @Subscribe
    fun onRoomMemberChange(update: SessionToCryptoRoomMembersUpdate) {
        taskExecutor.executorScope.launch(coroutineDispatchers.crypto) {
            val updatedTrust = computeTrustTask.execute(ComputeTrustTask.Params(update.userIds))
            // We need to send that back to session base

            BACKGROUND_HANDLER.post {
                backgroundSessionRealm.get().executeTransaction { realm ->
                    roomSummaryUpdater.updateShieldTrust(realm, update.roomId, updatedTrust)
                }
            }
        }
    }

    @Subscribe
    fun onTrustUpdate(update: CryptoToSessionUserTrustChange) {
        onCryptoDevicesChange(update.userIds)
    }

    private fun onCryptoDevicesChange(users: List<String>) {
        BACKGROUND_HANDLER.post {
            val impactedRoomsId = backgroundSessionRealm.get().where(RoomMemberSummaryEntity::class.java)
                    .`in`(RoomMemberSummaryEntityFields.USER_ID, users.toTypedArray())
                    .findAll()
                    .map { it.roomId }
                    .distinct()

            val map = HashMap<String, List<String>>()
            impactedRoomsId.forEach { roomId ->
                RoomMemberSummaryEntity.where(backgroundSessionRealm.get(), roomId)
                        .findAll()
                        .let { results ->
                            map[roomId] = results.map { it.userId }
                        }
            }

            map.forEach { entry ->
                val roomId = entry.key
                val userList = entry.value
                taskExecutor.executorScope.launch {
                    val updatedTrust = computeTrustTask.execute(ComputeTrustTask.Params(userList))
                    BACKGROUND_HANDLER.post {
                        backgroundSessionRealm.get().executeTransaction { realm ->
                            roomSummaryUpdater.updateShieldTrust(realm, roomId, updatedTrust)
                        }
                    }
                }
            }
        }
    }
}
