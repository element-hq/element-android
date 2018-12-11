package im.vector.matrix.android

import android.content.Context
import android.support.test.InstrumentationRegistry
import java.io.File

abstract class InstrumentedTest {
    fun context(): Context {
        return InstrumentationRegistry.getTargetContext()
    }

    fun cacheDir(): File {
        return context().cacheDir
    }
}