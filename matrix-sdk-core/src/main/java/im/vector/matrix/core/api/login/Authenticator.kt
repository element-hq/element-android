package im.vector.matrix.core.api.login

import im.vector.matrix.core.api.MatrixCallback
import im.vector.matrix.core.api.util.Cancelable
import im.vector.matrix.core.api.login.data.Credentials

interface Authenticator {

    fun authenticate(login: String, password: String, callback: MatrixCallback<Credentials>): Cancelable

}