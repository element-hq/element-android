package im.vector.matrix.core.internal

import im.vector.matrix.core.api.Session
import im.vector.matrix.core.api.login.Authenticator
import im.vector.matrix.core.api.login.data.HomeServerConnectionConfig
import im.vector.matrix.core.internal.login.LoginModule
import org.koin.core.scope.Scope
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.getKoin
import org.koin.standalone.inject

class DefaultSession(val homeServerConnectionConfig: HomeServerConnectionConfig) : Session, KoinComponent {

    private val authenticator by inject<Authenticator>()
    private val scope: Scope

    init {
        val loginModule = LoginModule(homeServerConnectionConfig)
        StandAloneContext.loadKoinModules(listOf(loginModule))
        scope = getKoin().createScope(SCOPE)
    }

    override fun authenticator(): Authenticator {
        return authenticator
    }

    override fun close() {
        scope.close()
    }

    companion object {
        const val SCOPE: String = "session"
    }


}