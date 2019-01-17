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