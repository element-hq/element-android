package im.vector.matrix.android.internal

import arrow.core.Try

interface Task<in PARAMS, out RESULT> {

    fun execute(params: PARAMS): Try<RESULT>

}