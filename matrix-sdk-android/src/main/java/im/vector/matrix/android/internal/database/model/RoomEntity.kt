package im.vector.matrix.android.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import kotlin.properties.Delegates

open class RoomEntity : RealmObject() {

    @PrimaryKey var roomId: String = ""
    var chunks: RealmList<ChunkEntity> = RealmList()
    private var membershipStr: String = Membership.NONE.name

    @delegate:Ignore var membership: Membership by Delegates.observable(Membership.valueOf(membershipStr)) { _, _, newValue ->
        membershipStr = newValue.name
    }

    companion object;

    enum class Membership {
        JOINED,
        LEFT,
        INVITED,
        NONE
    }
}

