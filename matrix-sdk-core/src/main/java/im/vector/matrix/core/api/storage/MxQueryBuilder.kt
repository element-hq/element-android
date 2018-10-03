package im.vector.matrix.core.api.storage

interface MxQueryBuilder<DATA, QUERY : MxQuery<DATA>> {

    fun build(): QUERY

}