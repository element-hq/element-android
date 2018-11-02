package im.vector.matrix.android.api

import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.koin.standalone.inject


class Matrix(matrixOptions: MatrixOptions) : KoinComponent {

    private val authenticator by inject<Authenticator>()
    private val backgroundDetectionObserver by inject<BackgroundDetectionObserver>()

    lateinit var currentSession: Session

    init {
        Monarchy.init(matrixOptions.context)

        val matrixModule = MatrixModule(matrixOptions)
        val networkModule = NetworkModule()
        val authModule = AuthModule()
        loadKoinModules(listOf(matrixModule, networkModule, authModule))

        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)

        val lastActiveSession = authenticator.getLastActiveSession()
        if (lastActiveSession != null) {
            currentSession = lastActiveSession
            currentSession.open()
        }
    }

    fun authenticator(): Authenticator {
        return authenticator
    }

}

data class MatrixOptions(val context: Context)
