package im.vector.matrix.android.internal.extensions

import com.squareup.sqldelight.Query
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

fun <T : Any> Flow<Query<T>>.mapToOneOptionnal(context: CoroutineContext): Flow<Optional<T>> {
    return map {
        withContext(context) {
            it.executeAsOneOrNull().toOptional()
        }
    }
}
