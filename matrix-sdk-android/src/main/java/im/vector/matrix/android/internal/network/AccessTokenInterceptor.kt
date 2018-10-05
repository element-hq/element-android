package im.vector.matrix.android.internal.network

import im.vector.matrix.android.api.auth.CredentialsStore
import okhttp3.Interceptor
import okhttp3.Response

class AccessTokenInterceptor(private val credentialsStore: CredentialsStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newRequestBuilder = request.newBuilder()
        // Add the access token to all requests if it is set
        val credentials = credentialsStore.get()
        credentials?.let {
            newRequestBuilder.addHeader("Authorization", "Bearer " + it.accessToken)
        }
        request = newRequestBuilder.build()
        return chain.proceed(request)
    }


}