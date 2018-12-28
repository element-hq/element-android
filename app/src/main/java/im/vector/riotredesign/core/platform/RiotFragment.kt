package im.vector.riotredesign.core.platform

import com.airbnb.mvrx.BaseMvRxFragment

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


}