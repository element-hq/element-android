package im.vector.matrix.android.internal.auth

import im.vector.matrix.android.internal.auth.data.SessionParams

interface SessionParamsStore {

    fun get(): SessionParams?

    fun save(sessionParams: SessionParams)

}