package im.vector.matrix.android.internal.database

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.zhuinden.monarchy.Monarchy
import io.realm.RealmObject
import io.realm.RealmResults
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
        val updateIndexes = changeSet.orderedCollectionChangeSet.changes + changeSet.orderedCollectionChangeSet.insertions
        val deletionIndexes = changeSet.orderedCollectionChangeSet.deletions
        process(changeSet.realmResults, updateIndexes, deletionIndexes)
    }

    abstract fun process(results: RealmResults<T>, updateIndexes: IntArray, deletionIndexes: IntArray)

}