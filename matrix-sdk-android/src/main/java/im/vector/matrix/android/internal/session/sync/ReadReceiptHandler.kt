/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject


// the receipts dictionnaries
// key   : $EventId
// value : dict key $UserId
//              value dict key ts
//                    dict value ts value
typealias ReadReceiptContent = Map<String, Map<String, Map<String, Map<String, Double>>>>

internal class ReadReceiptHandler @Inject constructor() {

    fun handle(realm: Realm, roomId: String, content: ReadReceiptContent?) {
        if (content == null) {
            return
        }
        try {
            handleReadReceiptContent(realm, roomId, content)
        } catch (exception: Exception) {
            Timber.e("Fail to handle read receipt for room $roomId")
        }
    }

    private fun handleReadReceiptContent(realm: Realm, roomId: String, content: ReadReceiptContent) {
        for ((eventId, receiptDict) in content) {
            val userIdsDict = receiptDict["m.read"] ?: continue
            val readReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId).findFirst()
                                      ?: realm.createObject(ReadReceiptsSummaryEntity::class.java, eventId)

            for ((userId, paramsDict) in userIdsDict) {
                val ts = paramsDict["ts"] ?: 0.0
                val primaryKey = "${roomId}_$userId"
                val receiptEntity = ReadReceiptEntity.where(realm, roomId, userId).findFirst()
                                    ?: realm.createObject(ReadReceiptEntity::class.java, primaryKey)

                ReadReceiptsSummaryEntity.where(realm, receiptEntity.eventId).findFirst()?.also {
                    it.readReceipts.remove(receiptEntity)
                }
                receiptEntity.apply {
                    this.eventId = eventId
                    this.roomId = roomId
                    this.userId = userId
                    this.originServerTs = ts
                }
                readReceiptsSummary.readReceipts.add(receiptEntity)
            }
        }
    }
}