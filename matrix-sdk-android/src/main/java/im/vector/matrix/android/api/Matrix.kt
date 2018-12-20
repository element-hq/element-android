package im.vector.matrix.android.api

import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MatrixKoinHolder
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import org.koin.standalone.inject


class Matrix private constructor(context: Context) : MatrixKoinComponent {

    private val authenticator by inject<Authenticator>()
    private val backgroundDetectionObserver by inject<BackgroundDetectionObserver>()
    lateinit var currentSession: Session

    init {
        Monarchy.init(context)
        val matrixModule = MatrixModule(context).definition
        val networkModule = NetworkModule().definition
        val authModule = AuthModule().definition
        MatrixKoinHolder.instance.loadModules(listOf(matrixModule, networkModule, authModule))
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

    companion object {
        private lateinit var instance: Matrix

        internal fun initialize(context: Context) {
            instance = Matrix(context.applicationContext)
        }

        fun getInstance(): Matrix {
            return instance
        }

    }

}
