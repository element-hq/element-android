package im.vector.matrix.android.internal.database

import com.zhuinden.monarchy.Monarchy

data class DatabaseInstances(
        val disk: Monarchy,
        val inMemory: Monarchy
)