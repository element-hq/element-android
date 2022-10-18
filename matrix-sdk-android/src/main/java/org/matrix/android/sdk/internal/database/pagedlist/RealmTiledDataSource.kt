package org.matrix.android.sdk.internal.database.pagedlist

import androidx.paging.DataSource
import androidx.paging.DataSource.InvalidatedCallback
import io.realm.kotlin.Realm
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.internal.database.RealmObjectMapper
import org.matrix.android.sdk.internal.database.RealmQueryBuilder

internal class RealmTiledDataSource<T : RealmObject, R> internal constructor(
        realm: Realm,
        liveQueryBuilder: LiveQueryBuilder<T>,
        private val mapper: RealmObjectMapper<T, R>,
        coroutineScope: CoroutineScope
) :
        TiledDataSource<R>() {

    class Factory<T : RealmObject, R>(
            private val realm: Realm,
            private val liveQueryBuilder: LiveQueryBuilder<T>,
            private val mapper: RealmObjectMapper<T, R>,
            private val coroutineScope: CoroutineScope,
    ) : DataSource.Factory<Int, R>() {

        override fun create(): DataSource<Int, R> {
            val childScope = CoroutineScope(coroutineScope.coroutineContext + SupervisorJob() + CoroutineName("RealmTiledDataSource"))
            return RealmTiledDataSource(realm, liveQueryBuilder, mapper, childScope)
        }
    }

    class LiveQueryBuilder<T : RealmObject>(queryBuilder: RealmQueryBuilder<T>) {

        var dataSource: DataSource<*, *>? = null

        var queryBuilder: RealmQueryBuilder<T> = queryBuilder
            set(value) {
                field = value
                dataSource?.invalidate()
            }
    }

    private val results: RealmResults<T>
    private val invalidatedCallback = InvalidatedCallback {
        liveQueryBuilder.dataSource = null
        coroutineScope.cancel()
    }

    init {
        addInvalidatedCallback(invalidatedCallback)
        liveQueryBuilder.dataSource = this
        results = liveQueryBuilder.queryBuilder.build(realm).find()
        results.asFlow()
                .onEach { resultsChange ->
                    when (resultsChange) {
                        is UpdatedResults -> {
                            invalidate()
                        }
                        else -> Unit
                    }
                }
                .onCompletion {
                    removeInvalidatedCallback(invalidatedCallback)
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
