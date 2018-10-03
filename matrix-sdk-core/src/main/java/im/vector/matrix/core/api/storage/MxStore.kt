package im.vector.matrix.core.api.storage

interface MxStore<DATA, KEY> {

    fun put(data: DATA)

    fun remove(data: DATA)

    fun get(id: KEY): DATA?

    fun getAll(): List<DATA>

}