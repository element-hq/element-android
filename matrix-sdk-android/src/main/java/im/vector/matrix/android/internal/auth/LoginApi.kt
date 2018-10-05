package im.vector.matrix.android.internal.auth

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.auth.data.PasswordLoginParams
import im.vector.matrix.android.internal.network.NetworkConstants
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * The login REST API.
 */
interface LoginApi {

    /**
     * Pass params to the server for the current login phase.
     *
     * @param loginParams the login parameters
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    fun login(@Body loginParams: PasswordLoginParams): Deferred<Response<Credentials>>

}
