package im.vector.matrix.android.internal.session

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.di.SessionModule
import im.vector.matrix.android.internal.events.sync.SyncModule
import im.vector.matrix.android.internal.events.sync.Synchronizer
import org.koin.core.scope.Scope
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.getKoin
import org.koin.standalone.inject

class DefaultSession(homeServerConnectionConfig: HomeServerConnectionConfig) : Session, KoinComponent {

    private val synchronizer by inject<Synchronizer>()
    private val scope: Scope

    init {
        val sessionModule = SessionModule(homeServerConnectionConfig)
        val syncModule = SyncModule()
        StandAloneContext.loadKoinModules(listOf(sessionModule, syncModule))
        scope = getKoin().createScope(SCOPE)
    }

    override fun synchronizer(): Synchronizer {
        return synchronizer
    }

    override fun close() {
        scope.close()
    }

    companion object {
        const val SCOPE: String = "session"
    }

}