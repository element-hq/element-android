package im.vector.matrix.android.api.auth

import im.vector.matrix.android.internal.auth.data.Credentials

interface CredentialsStore {

    fun get(): Credentials?

    fun save(credentials: Credentials)

}