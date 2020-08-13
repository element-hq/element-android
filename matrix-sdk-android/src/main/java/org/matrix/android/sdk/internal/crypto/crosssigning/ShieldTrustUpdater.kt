/*
 * Copyright 2020 New Vector Ltd
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
package org.matrix.android.sdk.internal.crypto.crosssigning

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

internal class ShieldTrustUpdater @Inject constructor(
        private val eventBus: EventBus,
        private val computeTrustTask: ComputeTrustTask,
        private val taskExecutor: TaskExecutor,
        @SessionDatabase private val sessionRealmConfiguration: RealmConfiguration,
        private val roomSummaryUpdater: RoomSummaryUpdater
): SessionLifecycleObserver {

    companion object {
        private val BACKGROUND_HANDLER = createBackgroundHandler("SHIELD_CRYPTO_DB_THREAD")
        private val BACKGROUND_HANDLER_DISPATCHER = BACKGROUND_HANDLER.asCoroutineDispatcher()
    }

    private val backgroundSessionRealm = AtomicReference<Realm>()

    private val isStarted = AtomicBoolean()

    override fun onStart() {
        if (isStarted.compareAndSet(false, true)) {
            eventBus.register(this)
            BACKGROUND_HANDLER.post {
                backgroundSessionRealm.set(Realm.getInstance(sessionRealmConfiguration))
            }
        }
    }

    override fun onStop() {
        if (isStarted.compareAndSet(true, false)) {
            eventBus.unregister(this)
            BACKGROUND_HANDLER.post {
                backgroundSessionRealm.getAndSet(null).also {
                    it?.close()
                }
            }
        }
    }

    @Subscribe
    fun onRoomMemberChange(update: SessionToCryptoRoomMembersUpdate) {
        if (!isStarted.get()) {
            return
        }
        taskExecutor.executorScope.launch(BACKGROUND_HANDLER_DISPATCHER) {
            val updatedTrust = computeTrustTask.execute(ComputeTrustTask.Params(update.userIds, update.isDirect))
            // We need to send that back to session base
            backgroundSessionRealm.get()?.executeTransaction { realm ->
                roomSummaryUpdater.updateShieldTrust(realm, update.roomId, updatedTrust)
            }
        }
    }

    @Subscribe
    fun onTrustUpdate(update: CryptoToSessionUserTrustChange) {
        if (!isStarted.get()) {
            return
        }
        onCryptoDevicesChange(update.userIds)
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
