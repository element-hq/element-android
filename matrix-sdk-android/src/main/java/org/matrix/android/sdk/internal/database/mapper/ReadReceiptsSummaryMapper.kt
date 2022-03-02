/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.mapper

import io.realm.Realm
import io.realm.RealmList
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import javax.inject.Inject

internal class ReadReceiptsSummaryMapper @Inject constructor(
        private val realmSessionProvider: RealmSessionProvider
) {

    fun map(readReceiptsSummaryEntity: ReadReceiptsSummaryEntity?): List<ReadReceipt> {
        if (readReceiptsSummaryEntity == null) {
            return emptyList()
        }
        val readReceipts = readReceiptsSummaryEntity.readReceipts
        // Avoid opening a new realm if we already have one opened
        return if (readReceiptsSummaryEntity.isManaged) {
            map(readReceipts, readReceiptsSummaryEntity.realm)
        } else {
            realmSessionProvider.withRealm { realm ->
                map(readReceipts, realm)
            }
        }
    }

    private fun map(readReceipts: RealmList<ReadReceiptEntity>, realm: Realm): List<ReadReceipt> {
        return readReceipts
                .mapNotNull {
                    val roomMember = RoomMemberSummaryEntity.where(realm, roomId = it.roomId, userId = it.userId).findFirst()
                            ?: return@mapNotNull null
                    ReadReceipt(roomMember.asDomain(), it.originServerTs.toLong())
                }
    }
}
