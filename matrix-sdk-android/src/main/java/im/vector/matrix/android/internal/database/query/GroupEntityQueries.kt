package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.model.GroupEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun GroupEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<GroupEntity> {
    return realm.where<GroupEntity>().equalTo(GroupEntityFields.GROUP_ID, roomId)
}

internal fun GroupEntity.Companion.where(realm: Realm, membership: MyMembership? = null): RealmQuery<GroupEntity> {
    val query = realm.where<GroupEntity>()
    if (membership != null) {
        query.equalTo(GroupEntityFields.MEMBERSHIP_STR, membership.name)
    }
    return query
}
