package im.vector.matrix.android.internal.auth.data

import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig

data class SessionParams(
        val credentials: Credentials,
        val homeServerConnectionConfig: HomeServerConnectionConfig
)
