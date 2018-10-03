package im.vector.matrix.core.internal.login.db

import im.vector.matrix.core.api.login.CredentialsStore
import im.vector.matrix.core.api.login.data.Credentials

class InMemoryCredentialsStore : CredentialsStore {

    var credentials: Credentials? = null

    override fun put(data: Credentials) = synchronized(this) {
        credentials = data.copy()
    }

    override fun remove(data: Credentials) = synchronized(this) {
        credentials = null
    }

    override fun get(id: String): Credentials? = synchronized(this) {
        return credentials
    }

    override fun getAll(): List<Credentials> = synchronized(this) {
        return credentials?.let { listOf(it) } ?: emptyList()
    }

}