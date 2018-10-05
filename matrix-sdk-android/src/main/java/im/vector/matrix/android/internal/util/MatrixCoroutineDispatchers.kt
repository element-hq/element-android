package im.vector.matrix.android.internal.util

import kotlinx.coroutines.CoroutineDispatcher

data class MatrixCoroutineDispatchers(
        val io: CoroutineDispatcher,
        val computation: CoroutineDispatcher,
        val main: CoroutineDispatcher
)