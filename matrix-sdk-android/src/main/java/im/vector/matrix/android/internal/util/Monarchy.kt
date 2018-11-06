package im.vector.matrix.android.internal.util

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm

fun Monarchy.tryTransactionSync(transaction: (realm: Realm) -> Unit): Try<Unit> {
    return Try {
        this.runTransactionSync(transaction)
    }
}

fun Monarchy.tryTransactionAsync(transaction: (realm: Realm) -> Unit): Try<Unit> {
    return Try {
        this.writeAsync(transaction)
    }
}