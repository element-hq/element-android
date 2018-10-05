package im.vector.matrix.android.api

import im.vector.matrix.android.api.auth.Authenticator

interface Session {

    fun authenticator(): Authenticator

    fun close()

}