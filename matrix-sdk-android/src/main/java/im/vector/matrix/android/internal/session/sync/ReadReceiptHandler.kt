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

import im.vector.matrix.sqldelight.session.SessionDatabase
import timber.log.Timber
import javax.inject.Inject

// the receipts dictionaries
// key   : $EventId
// value : dict key $UserId
//              value dict key ts
//                    dict value ts value
typealias ReadReceiptContent = Map<String, Map<String, Map<String, Map<String, Double>>>>

private const val READ_KEY = "m.read"
private const val TIMESTAMP_KEY = "ts"

internal class ReadReceiptHandler @Inject constructor(private val sessionDatabase: SessionDatabase) {

    companion object {

        fun createContent(userId: String, eventId: String): ReadReceiptContent {
            return mapOf(
                    eventId to mapOf(
                            READ_KEY to mapOf(
                                    userId to mapOf(
                                            TIMESTAMP_KEY to System.currentTimeMillis().toDouble()
                                    )
                            )
                    )
            )
        }
    }

    fun handle(roomId: String, content: ReadReceiptContent?, isInitialSync: Boolean) {
        if (content == null) {
            return
        }
        try {
            handleReadReceiptContent(roomId, content, isInitialSync)
        } catch (exception: Exception) {
            Timber.e("Fail to handle read receipt for room $roomId")
        }
    }

    private fun handleReadReceiptContent(roomId: String, content: ReadReceiptContent, isInitialSync: Boolean) {
        for ((eventId, receiptDict) in content) {
            val userIdsDict = receiptDict[READ_KEY] ?: continue
            for ((userId, paramsDict) in userIdsDict) {
                val ts = paramsDict[TIMESTAMP_KEY] ?: 0.0
                if (isInitialSync) {
                    sessionDatabase.readReceiptQueries.insert(roomId, eventId, userId, ts)
                } else {
                    // ensure new ts is superior to the previous one
                    val knownTs = sessionDatabase.readReceiptQueries.getTimestampForUser(roomId, userId).executeAsOneOrNull()
                    if (knownTs == null) {
                        sessionDatabase.readReceiptQueries.insert(roomId, eventId, userId, ts)
                    } else if (ts > knownTs) {
                        sessionDatabase.readReceiptQueries.updateReadReceipt(eventId, ts, roomId, userId)
                    }
                }
            }
        }
    }
}
