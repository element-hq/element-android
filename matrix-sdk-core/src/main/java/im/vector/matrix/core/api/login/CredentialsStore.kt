package im.vector.matrix.core.api.login

import im.vector.matrix.core.api.login.data.Credentials
import im.vector.matrix.core.api.storage.MxStore

interface CredentialsStore : MxStore<Credentials, String>