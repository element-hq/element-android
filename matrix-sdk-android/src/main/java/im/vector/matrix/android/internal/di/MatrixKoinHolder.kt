package im.vector.matrix.android.internal.di

import org.koin.core.Koin
import org.koin.core.KoinContext
import org.koin.standalone.KoinComponent

internal object MatrixKoinHolder {

    val instance: Koin by lazy {
        Koin.create()
    }

}

internal interface MatrixKoinComponent : KoinComponent {

    override fun getKoin(): KoinContext {
        return MatrixKoinHolder.instance.koinContext
    }

}