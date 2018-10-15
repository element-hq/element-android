package im.vector.matrix.android.internal.database

import io.realm.Realm
import io.realm.RealmConfiguration

class RealmInstanceHolder(realmConfiguration: RealmConfiguration) {

    val realm = Realm.getInstance(realmConfiguration)


}