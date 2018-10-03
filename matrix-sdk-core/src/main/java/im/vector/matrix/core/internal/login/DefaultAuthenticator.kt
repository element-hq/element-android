package im.vector.matrix.core.internal.login

import com.squareup.moshi.Moshi
import im.vector.matrix.core.api.MatrixCallback
import im.vector.matrix.core.api.login.Authenticator
import im.vector.matrix.core.api.login.CredentialsStore
import im.vector.matrix.core.api.login.data.Credentials
import im.vector.matrix.core.api.util.Cancelable
import im.vector.matrix.core.internal.MatrixCoroutineDispatchers
import im.vector.matrix.core.internal.util.map
import im.vector.matrix.core.internal.login.data.PasswordLoginParams
import im.vector.matrix.core.internal.network.executeRequest
import im.vector.matrix.core.internal.util.CancelableCoroutine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DefaultAuthenticator(private val loginApi: LoginApi,
                           private val jsonMapper: Moshi,
                           private val coroutineDispatchers: MatrixCoroutineDispatchers,
                           private val credentialsStore: CredentialsStore) : Authenticator {

    override fun authenticate(login: String, password: String, callback: MatrixCallback<Credentials>): Cancelable {
        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val loginParams = PasswordLoginParams.userIdentifier(login, password, "Mobile")
            val loginResult = executeRequest<Credentials> {
                apiCall = loginApi.login(loginParams)
                moshi = jsonMapper
                dispatcher = coroutineDispatchers.io
            }.map {
                it?.apply { credentialsStore.put(it) }
            }
            loginResult.either({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

}