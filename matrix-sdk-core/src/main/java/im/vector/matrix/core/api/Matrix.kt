package im.vector.matrix.core.api

import im.vector.matrix.core.api.login.data.HomeServerConnectionConfig
import im.vector.matrix.core.internal.DefaultSession
import im.vector.matrix.core.internal.MatrixModule
import im.vector.matrix.core.internal.network.NetworkModule
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
