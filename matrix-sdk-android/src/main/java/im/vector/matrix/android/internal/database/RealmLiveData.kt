package im.vector.matrix.android.internal.database

import android.arch.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

class RealmLiveData<T : RealmModel, U>(private val realmResults: RealmResults<T>,
                                       private val mapper: (T) -> U) : LiveData<List<U>>() {

    private val listener = RealmChangeListener<RealmResults<T>> { results ->
        value = results.map { mapper.invoke(it) }
    }

    override fun onActive() {
        realmResults.addChangeListener(listener)
    }

    override fun onInactive() {
        realmResults.removeChangeListener(listener)
    }
}