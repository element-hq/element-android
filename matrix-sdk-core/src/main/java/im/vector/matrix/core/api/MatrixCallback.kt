package im.vector.matrix.core.api

import im.vector.matrix.core.api.failure.Failure

interface MatrixCallback<in T> {

    fun onSuccess(data: T?)

    fun onFailure(failure: Failure)

}