package im.vector.matrix.android.internal.auth.db

import im.vector.matrix.android.api.auth.CredentialsStore
import im.vector.matrix.android.internal.auth.data.Credentials
import io.objectbox.Box

class ObjectBoxCredentialsStore(private val box: Box<Credentials>) : CredentialsStore {

    override fun save(credentials: Credentials) {
        box.put(credentials)
    }

    override fun get(): Credentials? {
        return box.all.lastOrNull()
    }

}