package im.vector.matrix.android.api.persitence

interface QueryBuilder<DATA, QUERY : Query<DATA>> {

    fun build(): QUERY

}