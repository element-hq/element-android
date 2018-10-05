package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.api.auth.CredentialsStore
import im.vector.matrix.android.api.auth.data.Credentials

class InMemoryCredentialsStore : CredentialsStore {

    var credentials: Credentials? = null

    override fun get(): Credentials? {
        return credentials
    }

    override fun save(credentials: Credentials) {
        this.credentials = credentials.copy()
    }

}