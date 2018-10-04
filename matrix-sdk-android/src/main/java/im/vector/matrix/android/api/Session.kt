package im.vector.matrix.android.api

import im.vector.matrix.android.api.login.Authenticator

interface Session {

    fun authenticator(): Authenticator

    fun close()

}