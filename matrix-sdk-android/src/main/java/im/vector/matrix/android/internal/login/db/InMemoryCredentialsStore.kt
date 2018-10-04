package im.vector.matrix.android.internal.login.db

import im.vector.matrix.android.api.login.CredentialsStore
import im.vector.matrix.android.api.login.data.Credentials

class InMemoryCredentialsStore : CredentialsStore {

    var credentials: Credentials? = null

    override fun get(): Credentials? {
        return credentials
    }

    override fun save(credentials: Credentials) {
        this.credentials = credentials.copy()
    }

}