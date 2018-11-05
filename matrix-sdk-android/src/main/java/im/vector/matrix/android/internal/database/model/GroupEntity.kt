package im.vector.matrix.android.internal.database.model

import im.vector.matrix.android.api.session.room.model.MyMembership
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kotlin.properties.Delegates

open class GroupEntity(@PrimaryKey var groupId: String = ""

) : RealmObject() {

    private var membershipStr: String = MyMembership.NONE.name

    @delegate:Ignore
    var membership: MyMembership by Delegates.observable(MyMembership.valueOf(membershipStr)) { _, _, newValue ->
        membershipStr = newValue.name
    }

    companion object

}