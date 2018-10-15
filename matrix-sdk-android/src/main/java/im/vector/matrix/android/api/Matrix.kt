package im.vector.matrix.android.api

import android.content.Context
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import io.realm.Realm
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.koin.standalone.inject
import timber.log.Timber
import timber.log.Timber.DebugTree


class Matrix(matrixOptions: MatrixOptions) : KoinComponent {

    private val authenticator by inject<Authenticator>()

    var currentSession: Session? = null

    init {
        Realm.init(matrixOptions.context)
        val matrixModule = MatrixModule(matrixOptions)
        val networkModule = NetworkModule()
        val authModule = AuthModule()
        loadKoinModules(listOf(matrixModule, networkModule, authModule))
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    fun authenticator(): Authenticator {
        return authenticator
    }

}

data class MatrixOptions(val context: Context)
