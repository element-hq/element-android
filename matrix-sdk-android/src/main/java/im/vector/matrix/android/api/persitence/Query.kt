package im.vector.matrix.android.api.persitence

import im.vector.matrix.android.api.util.Cancelable

interface Query<DATA> {

    fun find(): DATA

    fun subscribe(observer: QueryDataObserver<DATA>): Cancelable

}