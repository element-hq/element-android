package org.matrix.android.sdk.internal.database.pagedlist

import androidx.paging.DataSource
import io.realm.kotlin.Realm
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.internal.database.RealmObjectMapper
import org.matrix.android.sdk.internal.database.RealmQueryBuilder

internal class RealmTiledDataSource<T : RealmObject, R> internal constructor(
        realm: Realm,
        queryBuilder: RealmQueryBuilder<T>,
        private val mapper: RealmObjectMapper<T, R>,
        coroutineScope: CoroutineScope
) :
        TiledDataSource<R>() {

    class Factory<T : RealmObject, R>(
            private val realm: Realm,
            private val queryBuilder: RealmQueryBuilder<T>,
            private val mapper: RealmObjectMapper<T, R>,
            private val coroutineScope: CoroutineScope,
    ) : DataSource.Factory<Int, R>() {

        override fun create(): DataSource<Int, R> {
            val childScope = CoroutineScope(SupervisorJob() + coroutineScope.coroutineContext)
            return RealmTiledDataSource(realm, queryBuilder, mapper, childScope)
        }
    }

    private val results: RealmResults<T>

    init {
        addInvalidatedCallback {
            coroutineScope.coroutineContext.cancelChildren()
        }
        results = queryBuilder.build(realm).find()
        results.asFlow()
                .onEach { resultsChange ->
                    when (resultsChange) {
                        is UpdatedResults -> invalidate()
                        else -> Unit
                    }
                }
                .launchIn(coroutineScope)
    }

    override fun countItems(): Int {
        return results.size
    }

    override fun loadRange(startPosition: Int, count: Int): List<R> {
        val size = countItems()
        if (size == 0) return emptyList()
        return buildList {
            val endPosition = minOf(startPosition + count, size)
            for (position in startPosition until endPosition) {
                results.getOrNull(position)?.also { item ->
                    val mapped = mapper.map(item)
                    add(mapped)
                }
            }
        }
    }
}
