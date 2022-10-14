package org.matrix.android.sdk.internal.database

import androidx.lifecycle.asFlow
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedList.BoundaryCallback
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.R
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.pagedlist.RealmTiledDataSource

internal fun interface RealmQueryBuilder<T : RealmObject> {
    fun build(realm: TypedRealm): RealmQuery<T>
}

internal fun interface RealmSingleQueryBuilder<T : RealmObject> {
    fun build(realm: TypedRealm): RealmSingleQuery<T>
}

internal fun interface RealmObjectMapper<T : RealmObject, R> {
    fun map(realmObject: T): R
}

/**
 * This class is responsible for managing an instance of realm.
 * You should make sure to keep this class as a singleton.
 * You can force opening the realm by calling [open] method.
 * Otherwise it will be lazily opened when first querying/writing.
 * Makes sure to call [close] when you don't need the instance anymore.
 */
internal class RealmInstance(
        val coroutineScope: CoroutineScope,
        val realmConfiguration: RealmConfiguration,
        coroutineDispatcher: CoroutineDispatcher
) {

    private val realm =
            coroutineScope.async(context = coroutineDispatcher, start = CoroutineStart.LAZY) {
                Realm.open(realmConfiguration)
            }

    suspend fun open() {
        coroutineScope.launch {
            getRealm()
        }.join()
    }

    suspend fun close() = withContext(NonCancellable) {
        getRealm().close()
    }

    fun <T : RealmObject> queryResults(
            realmQueryBuilder: RealmQueryBuilder<T>
    ): Flow<ResultsChange<T>> {
        return getRealmFlow().flatMapConcat {
            realmQueryBuilder.build(it).asFlow()
        }
    }

    fun <T : RealmObject, R> queryList(
            mapper: RealmObjectMapper<T, R>,
            realmQueryBuilder: RealmQueryBuilder<T>
    ): Flow<List<R>> {
        return queryResults(realmQueryBuilder).map { resultChange ->
            resultChange.list.map { realmObject ->
                mapper.map(realmObject)
            }
        }
    }

    fun <T : RealmObject> queryFirst(
            realmQueryBuilder: RealmSingleQueryBuilder<T>
    ): Flow<Optional<T>> {
        return getRealmFlow().flatMapConcat {
            realmQueryBuilder.build(it).asFlow()
        }.map {
            Optional.from(it.obj)
        }
    }

    fun <T : RealmObject, R> queryPagedList(
            config: PagedList.Config,
            mapper: RealmObjectMapper<T, R>,
            boundaryCallback: BoundaryCallback<R>? = null,
            queryBuilder: RealmQueryBuilder<T>,
    ): Flow<PagedList<R>> {
        return queryUpdatablePagedList(
                config = config,
                mapper = mapper,
                boundaryCallback = boundaryCallback,
                liveQueryBuilder = flowOf(queryBuilder)
        )
    }

    fun <T : RealmObject, R> queryUpdatablePagedList(
            config: PagedList.Config,
            mapper: RealmObjectMapper<T, R>,
            boundaryCallback: BoundaryCallback<R>? = null,
            liveQueryBuilder: Flow<RealmQueryBuilder<T>>,
    ): Flow<PagedList<R>> {
        return getRealmFlow().flatMapConcat { realm ->
            val livePagedList = LivePagedListBuilder(
                    RealmTiledDataSource.Factory(
                            realm = realm,
                            liveQueryBuilder = liveQueryBuilder,
                            mapper = mapper,
                            coroutineScope = coroutineScope
                    ),
                    config
            ).apply {
                setBoundaryCallback(boundaryCallback)
            }.build()
            livePagedList.asFlow()
        }
    }

    suspend fun <R> write(block: MutableRealm.() -> R): R {
        return getRealm().write(block)
    }

    fun <R> blockingWrite(block: MutableRealm.() -> R): R {
        return runBlocking {
            write(block)
        }
    }

    fun asyncWrite(block: MutableRealm.() -> Unit) {
        coroutineScope.launch {
            write(block)
        }
    }

    suspend fun getRealm(): Realm = realm.await()

    fun getRealmFlow(): Flow<Realm> = flow {
        emit(getRealm())
    }

    fun getBlockingRealm(): Realm {
        return runBlocking {
            getRealm()
        }
    }
}
