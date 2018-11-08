package im.vector.matrix.android.internal.util

import kotlinx.coroutines.CoroutineDispatcher

internal data class MatrixCoroutineDispatchers(
        val io: CoroutineDispatcher,
        val computation: CoroutineDispatcher,
        val main: CoroutineDispatcher
)