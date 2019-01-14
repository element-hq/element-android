package im.vector.riotredesign.core.platform

import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.MvRx

abstract class RiotFragment : BaseMvRxFragment(), OnBackPressed {

    val riotActivity: RiotActivity by lazy {
        activity as RiotActivity
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun invalidate() {
        //no-ops by default
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args?.let { Bundle().apply { putParcelable(MvRx.KEY_ARG, it) } }
    }

}