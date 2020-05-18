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

import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.createBackgroundHandler
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@SessionScope
internal class ShieldTrustUpdater @Inject constructor(
        private val shieldTrustUpdaterInput: ShieldTrustUpdaterInput,
        private val computeTrustTask: ComputeTrustTask,
        private val taskExecutor: TaskExecutor,
        @SessionDatabase private val sessionRealmConfiguration: RealmConfiguration,
        private val roomSummaryUpdater: RoomSummaryUpdater
) : ShieldTrustUpdaterInput.Listener {

    companion object {
        private val BACKGROUND_HANDLER = createBackgroundHandler("SHIELD_CRYPTO_DB_THREAD")
        private val BACKGROUND_HANDLER_DISPATCHER = BACKGROUND_HANDLER.asCoroutineDispatcher()
    }

    private val backgroundSessionRealm = AtomicReference<Realm>()

    private val isStarted = AtomicBoolean()

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            shieldTrustUpdaterInput.listener = this
            BACKGROUND_HANDLER.post {
                backgroundSessionRealm.set(Realm.getInstance(sessionRealmConfiguration))
            }
        }
    }

    fun stop() {
        if (isStarted.compareAndSet(true, false)) {
            shieldTrustUpdaterInput.listener = null
            BACKGROUND_HANDLER.post {
                backgroundSessionRealm.getAndSet(null).also {
                    it?.close()
                }
            }
        }
    }

    override fun onSessionToCryptoRoomMembersUpdate(roomId: String, isDirect: Boolean, userIds: List<String>) {
        if (!isStarted.get()) {
            return
        }
        taskExecutor.executorScope.launch(BACKGROUND_HANDLER_DISPATCHER) {
            val updatedTrust = computeTrustTask.execute(ComputeTrustTask.Params(userIds, isDirect))
            // We need to send that back to session base
            backgroundSessionRealm.get()?.executeTransaction { realm ->
                roomSummaryUpdater.updateShieldTrust(realm, roomId, updatedTrust)
            }
        }
    }

    override fun onCryptoToSessionUserTrustChange(userIds: List<String>) {
        if (!isStarted.get()) {
            return
        }
        onCryptoDevicesChange(userIds)
    }

    private fun onCryptoDevicesChange(users: List<String>) {
        taskExecutor.executorScope.launch(BACKGROUND_HANDLER_DISPATCHER) {
            val realm = backgroundSessionRealm.get() ?: return@launch
            val distinctRoomIds = realm.where(RoomMemberSummaryEntity::class.java)
                    .`in`(RoomMemberSummaryEntityFields.USER_ID, users.toTypedArray())
                    .distinct(RoomMemberSummaryEntityFields.ROOM_ID)
                    .findAll()
                    .map { it.roomId }

            distinctRoomIds.forEach { roomId ->
                val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                if (roomSummary?.isEncrypted.orFalse()) {
                    val allActiveRoomMembers = RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
                    try {
                        val updatedTrust = computeTrustTask.execute(
                                ComputeTrustTask.Params(allActiveRoomMembers, roomSummary?.isDirect == true)
                        )
                        realm.executeTransaction {
                            roomSummaryUpdater.updateShieldTrust(it, roomId, updatedTrust)
                        }
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                    }
                }
            }
        }
    }
}
