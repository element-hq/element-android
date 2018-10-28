package im.vector.riotredesign.core.platform

import com.airbnb.mvrx.BaseMvRxFragment

abstract class RiotFragment : BaseMvRxFragment() {

    override fun invalidate() {
        //no-ops by default
    }


}