package org.matrix.android.sdk.internal.database.pagedlist

import androidx.paging.DataSource
import io.realm.kotlin.Realm
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class RealmTiledDataSource<T : RealmObject> internal constructor(
        realm: Realm,
        queryBuilder: (Realm) -> RealmQuery<T>,
        coroutineScope: CoroutineScope
) :
        TiledDataSource<T>() {

    class Factory<T : RealmObject>(
            private val realm: Realm,
            private val queryBuilder: (Realm) -> RealmQuery<T>,
            private val coroutineScope: CoroutineScope,
    ) : DataSource.Factory<Int, T>() {

        override fun create(): DataSource<Int, T> {
            val childScope = CoroutineScope(SupervisorJob() + coroutineScope.coroutineContext)
            return RealmTiledDataSource(realm, queryBuilder, childScope)
        }
    }

    private val results: RealmResults<T>

    init {
        addInvalidatedCallback {
            coroutineScope.coroutineContext.cancelChildren()
        }
        results = queryBuilder(realm).find()
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

    override fun loadRange(startPosition: Int, count: Int): List<T> {
        val size = countItems()
        if (size == 0) return emptyList()
        return buildList {
            val endPosition = minOf(startPosition + count, size)
            for (position in startPosition until endPosition) {
                results.getOrNull(position)?.also { item ->
                    add(item)
                }
            }
        }
    }
}
