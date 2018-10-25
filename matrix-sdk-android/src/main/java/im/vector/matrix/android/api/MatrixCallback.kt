package im.vector.matrix.android.api

import im.vector.matrix.android.api.failure.Failure

interface MatrixCallback<in T> {

    fun onSuccess(data: T) {
        //no-op
    }

    fun onFailure(failure: Failure){
        //no-op
    }

}