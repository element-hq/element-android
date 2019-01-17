package im.vector.matrix.rx

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.reactivex.Observable
import io.reactivex.android.MainThreadDisposable

private class LiveDataObservable<T>(
        private val liveData: LiveData<T>,
        private val valueIfNull: T? = null
) : Observable<T>() {

    override fun subscribeActual(observer: io.reactivex.Observer<in T>) {
        val relay = RemoveObserverInMainThread(observer)
        observer.onSubscribe(relay)
        liveData.observeForever(relay)
    }

    private inner class RemoveObserverInMainThread(private val observer: io.reactivex.Observer<in T>)
        : MainThreadDisposable(), Observer<T> {

        override fun onChanged(t: T?) {
            if (!isDisposed) {
                if (t == null) {
                    if (valueIfNull != null) {
                        observer.onNext(valueIfNull)
                    } else {
                        observer.onError(NullPointerException(
                                "convert liveData value t to RxJava onNext(t), t cannot be null"))
                    }
                } else {
                    observer.onNext(t)
                }
            }
        }

        override fun onDispose() {
            liveData.removeObserver(this)
        }
    }
}

fun <T> LiveData<T>.asObservable(): Observable<T> {
    return LiveDataObservable(this)
}