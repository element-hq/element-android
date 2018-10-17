package im.vector.matrix.android.internal.database

import android.support.annotation.MainThread
import io.realm.Realm
import io.realm.RealmConfiguration
import java.util.concurrent.atomic.AtomicBoolean

class SessionRealmHolder(private val realmConfiguration: RealmConfiguration
) {

    lateinit var instance: Realm
    private val isOpen = AtomicBoolean(false)

    @MainThread
    fun open() {
        if (isOpen.compareAndSet(false, true)) {
            instance = Realm.getInstance(realmConfiguration)
        }
    }

    @MainThread
    fun close() {
        if (isOpen.compareAndSet(true, false)) {
            instance.close()
            Realm.compactRealm(realmConfiguration)
        }
    }


}

