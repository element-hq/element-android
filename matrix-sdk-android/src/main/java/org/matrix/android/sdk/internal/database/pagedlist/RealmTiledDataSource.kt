package org.matrix.android.sdk.internal.database.pagedlist

import androidx.paging.DataSource
import io.realm.kotlin.Realm
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.matrix.android.sdk.internal.database.RealmObjectMapper
import org.matrix.android.sdk.internal.database.RealmQueryBuilder

internal class RealmTiledDataSource<T : RealmObject, R> internal constructor(
        realm: Realm,
        liveQueryBuilder: Flow<RealmQueryBuilder<T>>,
        private val mapper: RealmObjectMapper<T, R>,
        coroutineScope: CoroutineScope
) :
        TiledDataSource<R>() {

    class Factory<T : RealmObject, R>(
            private val realm: Realm,
            private val liveQueryBuilder: Flow<RealmQueryBuilder<T>>,
            private val mapper: RealmObjectMapper<T, R>,
            private val coroutineScope: CoroutineScope,
    ) : DataSource.Factory<Int, R>() {

        override fun create(): DataSource<Int, R> {
            val childScope = CoroutineScope(SupervisorJob() + coroutineScope.coroutineContext)
            return RealmTiledDataSource(realm, liveQueryBuilder, mapper, childScope)
        }
    }

    private var results: List<T> = emptyList()

    init {
        addInvalidatedCallback {
            coroutineScope.coroutineContext.cancelChildren()
        }
        liveQueryBuilder
                .take(1)
                .flatMapConcat {
                    it.build(realm).asFlow()
                }
                .onEach { resultsChange ->
                    when (resultsChange) {
                        is UpdatedResults -> invalidate()
                        else -> Unit
                    }
                }.onCompletion {
                    invalidate()
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
