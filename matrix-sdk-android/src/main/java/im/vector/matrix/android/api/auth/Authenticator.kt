package im.vector.matrix.android.api.auth

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable

interface Authenticator {

    fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig, login: String, password: String, callback: MatrixCallback<Session>): Cancelable

    fun hasActiveSessions(): Boolean

    fun getLastActiveSession(): Session?


}