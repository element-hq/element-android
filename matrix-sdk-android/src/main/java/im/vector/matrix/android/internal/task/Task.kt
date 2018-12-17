package im.vector.matrix.android.internal.task

import arrow.core.Try

internal interface Task<PARAMS, RESULT> {

    fun execute(params: PARAMS): Try<RESULT>

}

