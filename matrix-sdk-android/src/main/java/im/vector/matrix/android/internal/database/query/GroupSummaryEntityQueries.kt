package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun GroupSummaryEntity.Companion.where(realm: Realm, groupId: String? = null): RealmQuery<GroupSummaryEntity> {
    val query = realm.where<GroupSummaryEntity>()
    if (groupId != null) {
        query.equalTo(GroupSummaryEntityFields.GROUP_ID, groupId)
    }
    return query
}

