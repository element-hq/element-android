package im.vector.matrix.android.api

interface MatrixCallback<in T> {

    fun onSuccess(data: T) {
        //no-op
    }

    fun onFailure(failure: Throwable) {
        //no-op
    }

}