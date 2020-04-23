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

import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.createBackgroundHandler
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal class ShieldTrustUpdater @Inject constructor(
        private val eventBus: EventBus,
        private val computeTrustTask: ComputeTrustTask,
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sessionDatabase: SessionDatabase) {

    companion object {
        private val BACKGROUND_HANDLER = createBackgroundHandler("SHIELD_CRYPTO_DB_THREAD")
    }

    private val isStarted = AtomicBoolean()

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            eventBus.register(this)
        }
    }

    fun stop() {
        if (isStarted.compareAndSet(true, false)) {
            eventBus.unregister(this)
        }
    }

    @Subscribe
    fun onRoomMemberChange(update: SessionToCryptoRoomMembersUpdate) {
        if (!isStarted.get()) {
            return
        }
        taskExecutor.executorScope.updateTrustLevel(update.roomId, update.userIds)
    }

    @Subscribe
    fun onTrustUpdate(update: CryptoToSessionUserTrustChange) {
        if (!isStarted.get()) {
            return
        }
        onCryptoDevicesChange(update.userIds)
    }

    private fun onCryptoDevicesChange(users: List<String>) {
        BACKGROUND_HANDLER.post {
            val impactedRoomsId = sessionDatabase.roomMemberSummaryQueries.getAllRoomIdsFromUsers(users).executeAsList()
            impactedRoomsId.forEach { roomId ->
                val allUsersFromRoom = sessionDatabase.roomMemberSummaryQueries.getAllUserIdFromRoom(
                        memberships = Membership.all().map(),
                        excludedIds = emptyList(),
                        roomId = roomId
                ).executeAsList()
                taskExecutor.executorScope.updateTrustLevel(roomId, allUsersFromRoom)
            }
        }
    }

    private fun CoroutineScope.updateTrustLevel(roomId: String, userIds: List<String>) {
        launch(coroutineDispatchers.dbTransaction) {
            try {
                // Can throw if the crypto database has been closed in between, in this case log and ignore?
                val updatedTrust = computeTrustTask.execute(ComputeTrustTask.Params(userIds))
                sessionDatabase.roomSummaryQueries.setRoomEncryptionTrustLevel(updatedTrust.name, roomId)
            } catch (failure: Throwable) {
                Timber.e(failure)
            }
        }
    }
}
