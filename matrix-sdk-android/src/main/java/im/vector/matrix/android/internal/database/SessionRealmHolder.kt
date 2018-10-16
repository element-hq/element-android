package im.vector.matrix.android.internal.database

import android.support.annotation.MainThread
import io.realm.Realm
import io.realm.RealmConfiguration

class SessionRealmHolder(private val realmConfiguration: RealmConfiguration
) {

    lateinit var instance: Realm

    @MainThread
    fun open() {
        instance = Realm.getInstance(realmConfiguration)
    }

    @MainThread
    fun close() {
        instance.close()
        Realm.compactRealm(realmConfiguration)
    }


}

