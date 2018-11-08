package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class ReadReceiptEntity(@PrimaryKey var primaryKey: String = "",
                             var userId: String = "",
                             var eventId: String = "",
                             var roomId: String = "",
                             var originServerTs: Double = 0.0
) : RealmObject() {
    companion object
}
