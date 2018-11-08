package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class SyncEntity(var nextBatch: String? = null,
                      @PrimaryKey var id: Long = 0
) : RealmObject()