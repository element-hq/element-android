package im.vector.riotredesign.core.platform

import com.airbnb.mvrx.BaseMvRxActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RiotActivity : BaseMvRxActivity() {

    private val uiDisposables = CompositeDisposable()

    protected fun Disposable.disposeOnDestroy(): Disposable {
        uiDisposables.add(this)
        return this
    }

}