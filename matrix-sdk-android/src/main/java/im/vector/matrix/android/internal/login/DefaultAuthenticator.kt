package im.vector.matrix.android.internal.login

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.login.Authenticator
import im.vector.matrix.android.api.login.CredentialsStore
import im.vector.matrix.android.api.login.data.Credentials
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.map
import im.vector.matrix.android.internal.login.data.PasswordLoginParams
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.CancelableCoroutine
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
                it?.apply { credentialsStore.save(it) }
            }
            loginResult.either({ callback.onFailure(it) }, { callback.onSuccess(it) })
        }
        return CancelableCoroutine(job)
    }

}