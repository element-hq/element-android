package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.SyncEntity

internal class SyncTokenStore(private val monarchy: Monarchy) {

    fun getLastToken(): String? {
        var token: String? = null
        monarchy.doWithRealm { realm ->
            token = realm.where(SyncEntity::class.java).findFirst()?.nextBatch
        }
        return token
    }

    fun saveToken(token: String?) {
        monarchy.writeAsync {
            val sync = SyncEntity(token)
            it.insertOrUpdate(sync)
        }
    }


}