package org.matrix.android.sdk.internal.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.pagedlist.RealmTiledDataSource

internal typealias RealmQueryBuilder<T> = (TypedRealm) -> RealmQuery<T>

/**
 * This class is responsible for managing an instance of realm.
 * You should make sure to keep this class as a singleton.
 * You can force opening the realm by calling [open] method.
 * Otherwise it will be lazily opened when first querying/writing.
 * Makes sure to call [close] when you don't need the instance anymore.
 */
internal class RealmInstance(
        val coroutineScope: CoroutineScope,
        private val realmConfiguration: RealmConfiguration,
        coroutineDispatcher: CoroutineDispatcher
) {

    private val realm =
            coroutineScope.async(context = coroutineDispatcher, start = CoroutineStart.LAZY) {
                Realm.open(realmConfiguration)
            }

    suspend fun open() {
        coroutineScope.launch {
            realm.await()
        }.join()
    }

    suspend fun close() = withContext(NonCancellable) {
        realm.await().close()
    }

    fun <T : RealmObject> queryResults(
            realmQueryBuilder: RealmQueryBuilder<T>
    ): Flow<ResultsChange<T>> {
        return getRealmFlow().flatMapConcat {
            realmQueryBuilder(it).asFlow()
        }
    }

    fun <T : RealmObject> queryList(
            realmQueryBuilder: RealmQueryBuilder<T>
    ): Flow<List<T>> {
        return queryResults(realmQueryBuilder).map {
            it.list
        }
    }

    fun <T : RealmObject> queryFirst(
            realmQueryBuilder: RealmQueryBuilder<T>
    ): Flow<Optional<T>> {
        return getRealmFlow().flatMapConcat {
            realmQueryBuilder(it).first().asFlow()
        }.map {
            Optional.from(it.obj)
        }
    }

    fun <T : RealmObject> queryPagedList(
            config: PagedList.Config,
            queryBuilder: RealmQueryBuilder<T>
    ): Flow<PagedList<T>> {

        fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {
            val observer = Observer<T> { value -> trySend(value) }
            observeForever(observer)
            awaitClose {
                removeObserver(observer)
            }
        }.flowOn(Dispatchers.Main.immediate)

        return getRealmFlow().flatMapConcat { realm ->
            val livePagedList = LivePagedListBuilder(
                    RealmTiledDataSource.Factory(
                            realm = realm,
                            queryBuilder = queryBuilder,
                            coroutineScope = coroutineScope
                    ),
                    config
            ).build()
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



