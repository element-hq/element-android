package im.vector.riotredesign.core.platform

import com.airbnb.mvrx.BaseMvRxFragment

abstract class RiotFragment : BaseMvRxFragment() {

    val riotActivity: RiotActivity by lazy {
        activity as RiotActivity
    }


    override fun invalidate() {
        //no-ops by default
    }


}