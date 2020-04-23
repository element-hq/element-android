package im.vector.matrix.android.internal.concurrency

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class NamedThreadFactory(private val name: String) : ThreadFactory {

    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable, name)
    }
}

internal fun newNamedSingleThreadExecutor(name: String): Executor {
    return Executors.newSingleThreadExecutor(NamedThreadFactory(name))
}
