package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.api.auth.SessionParamsStore
import im.vector.matrix.android.internal.auth.data.SessionParams
import io.objectbox.Box

class ObjectBoxSessionParamsStore(private val mapper: ObjectBoxSessionParamsMapper,
                                  private val box: Box<ObjectBoxSessionParams>) : SessionParamsStore {

    override fun save(sessionParams: SessionParams) {
        val objectBoxSessionParams = mapper.map(sessionParams)
        objectBoxSessionParams?.let {
            box.put(it)
        }
    }

    override fun get(): SessionParams? {
        return box.all.map { mapper.map(it) }.lastOrNull()
    }

}