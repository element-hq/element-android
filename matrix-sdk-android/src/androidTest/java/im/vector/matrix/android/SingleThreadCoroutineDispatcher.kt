package im.vector.matrix.android

import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.Dispatchers.Main

internal val testCoroutineDispatchers = MatrixCoroutineDispatchers(Main, Main, Main)