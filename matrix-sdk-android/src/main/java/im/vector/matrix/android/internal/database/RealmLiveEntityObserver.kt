/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.database

import com.zhuinden.monarchy.Monarchy
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmObject
import io.realm.RealmResults
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal interface LiveEntityObserver {
    fun start()
    fun dispose()
    fun isStarted(): Boolean
}

internal abstract class RealmLiveEntityObserver<T : RealmObject>(protected val monarchy: Monarchy)
    : LiveEntityObserver {

    protected abstract val query: Monarchy.Query<T>
    private val isStarted = AtomicBoolean(false)
    private lateinit var results: AtomicReference<RealmResults<T>>

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            monarchy.postToMonarchyThread {
                val queryResults = query.createQuery(it).findAll()
                queryResults.addChangeListener { t, changeSet ->
                    onChanged(t, changeSet)
                }
                results = AtomicReference(queryResults)
            }
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            monarchy.postToMonarchyThread {
                results.getAndSet(null).removeAllChangeListeners()
            }
        }
    }

    override fun isStarted(): Boolean {
        return isStarted.get()
    }

    private fun onChanged(realmResults: RealmResults<T>, changeSet: OrderedCollectionChangeSet) {
        val insertionIndexes = changeSet.insertions
        val updateIndexes = changeSet.changes
        val deletionIndexes = changeSet.deletions
        val inserted = realmResults.filterIndexed { index, _ -> insertionIndexes.contains(index) }
        val updated = realmResults.filterIndexed { index, _ -> updateIndexes.contains(index) }
        val deleted = realmResults.filterIndexed { index, _ -> deletionIndexes.contains(index) }
        processChanges(inserted, updated, deleted)
    }

    protected abstract fun processChanges(inserted: List<T>, updated: List<T>, deleted: List<T>)

}