package im.vector.matrix.android.api

import im.vector.matrix.android.api.login.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.DefaultSession
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import org.koin.standalone.StandAloneContext.loadKoinModules

class Matrix(matrixOptions: MatrixOptions) {

    init {
        val matrixModule = MatrixModule(matrixOptions)
        val networkModule = NetworkModule()
        loadKoinModules(listOf(matrixModule, networkModule))
    }

    fun createSession(homeServerConnectionConfig: HomeServerConnectionConfig): Session {
        return DefaultSession(homeServerConnectionConfig)
    }

}
