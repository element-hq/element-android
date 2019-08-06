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

package im.vector.matrix.android.internal.session.room.send

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

internal class LocalEchoUpdater @Inject constructor(private val monarchy: Monarchy) {

    suspend fun updateSendState(eventId: String, sendState: SendState) {
        Timber.v("Update local state of $eventId to ${sendState.name}")
        monarchy.awaitTransaction { realm ->
            val sendingEventEntity = EventEntity.where(realm, eventId).findFirst()
            if (sendingEventEntity != null) {
                if (sendState == SendState.SENT && sendingEventEntity.sendState == SendState.SYNCED) {
                    //If already synced, do not put as sent
                } else {
                    sendingEventEntity.sendState = sendState
                }
            }
        }
    }

}