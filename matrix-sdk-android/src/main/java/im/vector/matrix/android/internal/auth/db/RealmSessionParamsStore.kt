package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.auth.data.SessionParams
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmSessionParamsStore(private val mapper: SessionParamsMapper,
                              private val realmConfiguration: RealmConfiguration) : SessionParamsStore {

    override fun save(sessionParams: SessionParams) {
        val entity = mapper.map(sessionParams)
        if (entity != null) {
            val realm = Realm.getInstance(realmConfiguration)
            realm.executeTransaction {
                it.insert(entity)
            }
            realm.close()
        }
    }

    override fun get(): SessionParams? {
        val realm = Realm.getInstance(realmConfiguration)
        val sessionParams = realm
                .where(SessionParamsEntity::class.java)
                .findAll()
                .map { mapper.map(it) }
                .lastOrNull()
        realm.close()
        return sessionParams
    }

}