package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.sqldelight.session.Memberships

fun Memberships.map(): Membership {
    return Membership.valueOf(name)
}

fun Membership.map():Memberships {
    return Memberships.valueOf(name)
}

fun List<Membership>.map(): List<Memberships> = this.map {
    it.map()
}
