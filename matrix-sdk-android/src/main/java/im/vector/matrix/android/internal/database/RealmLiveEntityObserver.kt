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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.zhuinden.monarchy.Monarchy
import io.realm.RealmObject
import java.util.concurrent.atomic.AtomicBoolean

internal interface LiveEntityObserver {
    fun start()
    fun dispose()
}

internal abstract class RealmLiveEntityObserver<T : RealmObject>(protected val monarchy: Monarchy)
    : Observer<Monarchy.ManagedChangeSet<T>>, LiveEntityObserver {

    protected abstract val query: Monarchy.Query<T>
    private val isStarted = AtomicBoolean(false)
    private val liveResults: LiveData<Monarchy.ManagedChangeSet<T>> by lazy {
        monarchy.findAllManagedWithChanges(query)
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            liveResults.observeForever(this)
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            liveResults.removeObserver(this)
        }
    }

    // PRIVATE

    override fun onChanged(changeSet: Monarchy.ManagedChangeSet<T>?) {
        if (changeSet == null) {
            return
        }
        val insertionIndexes = changeSet.orderedCollectionChangeSet.insertions
        val updateIndexes = changeSet.orderedCollectionChangeSet.changes
        val deletionIndexes = changeSet.orderedCollectionChangeSet.deletions
        val inserted = changeSet.realmResults.filterIndexed { index, _ -> insertionIndexes.contains(index) }
        val updated = changeSet.realmResults.filterIndexed { index, _ -> updateIndexes.contains(index) }
        val deleted = changeSet.realmResults.filterIndexed { index, _ -> deletionIndexes.contains(index) }
        process(inserted, updated, deleted)
    }

    abstract fun process(inserted: List<T>, updated: List<T>, deleted: List<T>)

}