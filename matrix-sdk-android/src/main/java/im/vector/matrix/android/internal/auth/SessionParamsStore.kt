package im.vector.matrix.android.internal.auth

import arrow.core.Try
import im.vector.matrix.android.api.auth.data.SessionParams

internal interface SessionParamsStore {

    fun get(): SessionParams?

    fun save(sessionParams: SessionParams): Try<SessionParams>

}