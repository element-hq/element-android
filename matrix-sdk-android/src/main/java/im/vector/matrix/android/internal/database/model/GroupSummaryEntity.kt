package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class GroupSummaryEntity(@PrimaryKey var groupId: String = "",
                              var displayName: String = "",
                              var shortDescription: String = "",
                              var avatarUrl: String = ""
) : RealmObject() {

    companion object

}