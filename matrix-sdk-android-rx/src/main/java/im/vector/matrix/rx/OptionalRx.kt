package im.vector.matrix.rx

import im.vector.matrix.android.api.util.Optional
import io.reactivex.Observable

fun <T : Any> Observable<Optional<T>>.unwrap(): Observable<T> {
    return this
            .filter { it.hasValue() }
            .map { it.get() }
}