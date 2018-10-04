package im.vector.matrix.android.api

import im.vector.matrix.android.api.failure.Failure

interface MatrixCallback<in T> {

    fun onSuccess(data: T?)

    fun onFailure(failure: Failure)

}