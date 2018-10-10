package im.vector.matrix.android.internal.auth.db

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.auth.data.Credentials
import im.vector.matrix.android.internal.auth.data.SessionParams

class ObjectBoxSessionParamsMapper(moshi: Moshi) {

    private val credentialsAdapter = moshi.adapter(Credentials::class.java)
    private val homeServerConnectionConfigAdapter = moshi.adapter(HomeServerConnectionConfig::class.java)

    fun map(objectBoxSessionParams: ObjectBoxSessionParams?): SessionParams? {
        if (objectBoxSessionParams == null) {
            return null
        }
        val credentials = credentialsAdapter.fromJson(objectBoxSessionParams.credentialsJson)
        val homeServerConnectionConfig = homeServerConnectionConfigAdapter.fromJson(objectBoxSessionParams.homeServerConnectionConfigJson)
        if (credentials == null || homeServerConnectionConfig == null) {
            return null
        }
        return SessionParams(credentials, homeServerConnectionConfig)
    }

    fun map(sessionParams: SessionParams?): ObjectBoxSessionParams? {
        if (sessionParams == null) {
            return null
        }
        val credentialsJson = credentialsAdapter.toJson(sessionParams.credentials)
        val homeServerConnectionConfigJson = homeServerConnectionConfigAdapter.toJson(sessionParams.homeServerConnectionConfig)
        if (credentialsJson == null || homeServerConnectionConfigJson == null) {
            return null
        }
        return ObjectBoxSessionParams(credentialsJson, homeServerConnectionConfigJson)
    }


}