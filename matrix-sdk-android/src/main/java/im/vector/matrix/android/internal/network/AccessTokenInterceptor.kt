package im.vector.matrix.android.internal.network

import im.vector.matrix.android.internal.auth.SessionParamsStore
import okhttp3.Interceptor
import okhttp3.Response

class AccessTokenInterceptor(private val sessionParamsStore: SessionParamsStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newRequestBuilder = request.newBuilder()
        // Add the access token to all requests if it is set
        val sessionParams = sessionParamsStore.get()
        sessionParams?.let {
            newRequestBuilder.addHeader("Authorization", "Bearer " + it.credentials.accessToken)
        }
        request = newRequestBuilder.build()
        return chain.proceed(request)
    }


}