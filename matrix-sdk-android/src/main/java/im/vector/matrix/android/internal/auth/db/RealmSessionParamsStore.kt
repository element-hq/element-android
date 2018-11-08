package im.vector.matrix.android.internal.auth.db

import arrow.core.Try
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.api.auth.data.SessionParams
import io.realm.Realm
import io.realm.RealmConfiguration

internal class RealmSessionParamsStore(private val mapper: SessionParamsMapper,
                              private val realmConfiguration: RealmConfiguration) : SessionParamsStore {

    override fun save(sessionParams: SessionParams): Try<SessionParams> {
        return Try {
            val entity = mapper.map(sessionParams)
            if (entity != null) {
                val realm = Realm.getInstance(realmConfiguration)
                realm.executeTransaction {
                    it.insert(entity)
                }
                realm.close()
            }
            sessionParams
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