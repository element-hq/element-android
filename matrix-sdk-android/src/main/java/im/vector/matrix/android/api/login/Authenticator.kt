package im.vector.matrix.android.api.login

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.login.data.Credentials

interface Authenticator {

    fun authenticate(login: String, password: String, callback: MatrixCallback<Credentials>): Cancelable

}