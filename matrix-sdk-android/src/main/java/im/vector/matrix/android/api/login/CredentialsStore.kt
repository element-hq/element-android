package im.vector.matrix.android.api.login

import im.vector.matrix.android.api.login.data.Credentials

interface CredentialsStore {

    fun get(): Credentials?

    fun save(credentials: Credentials)

}