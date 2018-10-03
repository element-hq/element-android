package im.vector.matrix.core.api.storage

import im.vector.matrix.core.api.util.Cancelable

interface MxQuery<DATA> {

    fun find(): DATA?

    fun subscribe(observer: MxQueryDataObserver<DATA>): Cancelable

}