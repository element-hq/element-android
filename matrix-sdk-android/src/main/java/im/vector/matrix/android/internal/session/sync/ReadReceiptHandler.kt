package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import io.realm.Realm


// the receipts dictionnaries
// key   : $EventId
// value : dict key $UserId
//              value dict key ts
//                    dict value ts value
typealias ReadReceiptContent = Map<String, Map<String, Map<String, Map<String, Double>>>>

internal class ReadReceiptHandler {

    fun handle(realm: Realm, roomId: String, content: ReadReceiptContent?): List<ReadReceiptEntity> {
        if (content == null) {
            return emptyList()
        }
        val readReceipts = content
                .flatMap { (eventId, receiptDict) ->
                    receiptDict
                            .filterKeys { it == "m.read" }
                            .flatMap { (_, userIdsDict) ->
                                userIdsDict.map { (userId, paramsDict) ->
                                    val ts = paramsDict.filterKeys { it == "ts" }
                                            .values
                                            .firstOrNull() ?: 0.0
                                    val primaryKey = roomId + userId
                                    ReadReceiptEntity(primaryKey, userId, eventId, roomId, ts)
                                }
                            }
                }
        realm.insertOrUpdate(readReceipts)
        return readReceipts
    }

}