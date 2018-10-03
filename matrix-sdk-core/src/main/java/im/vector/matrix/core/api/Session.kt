package im.vector.matrix.core.api

import im.vector.matrix.core.api.login.Authenticator

interface Session {

    fun authenticator(): Authenticator

    fun close()

}