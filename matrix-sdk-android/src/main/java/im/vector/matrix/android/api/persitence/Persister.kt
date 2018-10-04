package im.vector.matrix.android.api.persitence

import android.arch.paging.PagedList

interface Persister<DATA, KEY> {

    fun put(data: DATA)

    fun remove(data: DATA)

    fun get(id: KEY): DATA?

    fun getAll(): PagedList<DATA>

}