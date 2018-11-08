package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun ReadReceiptEntity.Companion.where(realm: Realm, roomId: String, userId: String): RealmQuery<ReadReceiptEntity> {
    return realm.where<ReadReceiptEntity>()
            .equalTo(ReadReceiptEntityFields.ROOM_ID, roomId)
            .equalTo(ReadReceiptEntityFields.USER_ID, userId)
}