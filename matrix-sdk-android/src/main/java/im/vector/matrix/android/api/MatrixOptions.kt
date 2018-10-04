package im.vector.matrix.android.api

import android.content.Context
import im.vector.matrix.android.api.thread.MainThreadExecutor
import java.util.concurrent.Executor

data class MatrixOptions(val context: Context,
                         val mainExecutor: Executor = MainThreadExecutor())