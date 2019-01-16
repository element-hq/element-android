package im.vector.matrix.android

import android.content.Context
import androidx.test.InstrumentationRegistry
import java.io.File

interface InstrumentedTest {
    fun context(): Context {
        return InstrumentationRegistry.getTargetContext()
    }

    fun cacheDir(): File {
        return context().cacheDir
    }
}