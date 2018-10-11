package im.vector.matrix.android.internal.auth

import arrow.core.leftIfNull
import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.SessionParamsStore
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.data.Credentials
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.auth.data.SessionParams
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.util.CancelableCoroutine
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit

class DefaultAuthenticator(private val retrofitBuilder: Retrofit.Builder,
                           private val jsonMapper: Moshi,
                           private val coroutineDispatchers: MatrixCoroutineDispatchers,
                           private val sessionParamsStore: SessionParamsStore) : Authenticator {

    override fun hasActiveSessions(): Boolean {
        return sessionParamsStore.get() != null
    }

    override fun getLastActiveSession(): Session? {
        val sessionParams = sessionParamsStore.get()
        return sessionParams?.let {
            DefaultSession(it)
        }
    }

    override fun authenticate(homeServerConnectionConfig: HomeServerConnectionConfig, login: String, password: String, callback: MatrixCallback<Session>): Cancelable {
        val authAPI = buildAuthAPI(homeServerConnectionConfig)

        val job = GlobalScope.launch(coroutineDispatchers.main) {
            val loginParams = PasswordLoginParams.userIdentifier(login, password, "Mobile")
            executeRequest<Credentials> {
                apiCall = authAPI.login(loginParams)
                moshi = jsonMapper
                dispatcher = coroutineDispatchers.io
            }.leftIfNull {
                Failure.Unknown(IllegalArgumentException("Credentials shouldn't not be null"))
            }.map {
                val sessionParams = SessionParams(it, homeServerConnectionConfig)
                sessionParamsStore.save(sessionParams)
                sessionParams
            }.map {
                DefaultSession(it)
            }.bimap(
                    { callback.onFailure(it) }, { callback.onSuccess(it) }
            )
        }
        return CancelableCoroutine(job)
    }

    private fun buildAuthAPI(homeServerConnectionConfig: HomeServerConnectionConfig): AuthAPI {
        val retrofit = retrofitBuilder.baseUrl(homeServerConnectionConfig.homeServerUri.toString()).build()
        return retrofit.create(AuthAPI::class.java)
    }


}