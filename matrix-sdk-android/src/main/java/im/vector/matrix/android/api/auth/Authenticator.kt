package im.vector.matrix.android.api.auth

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.auth.data.Credentials

interface Authenticator {

    fun authenticate(login: String, password: String, callback: MatrixCallback<Credentials>): Cancelable

}