package im.vector.matrix.android.internal.events.sync

import im.vector.matrix.android.internal.database.model.SyncEntity
import io.realm.Realm
import io.realm.RealmConfiguration

class SyncTokenStore(private val realmConfiguration: RealmConfiguration) {

    fun getLastToken(): String? {
        val realm = Realm.getInstance(realmConfiguration)
        val token = realm.where(SyncEntity::class.java).findFirst()?.nextBatch
        realm.close()
        return token
    }

    fun saveToken(token: String?) {
        val realm = Realm.getInstance(realmConfiguration)
        realm.executeTransaction {
            val sync = SyncEntity(token)
            it.insertOrUpdate(sync)
        }
        realm.close()
    }


}