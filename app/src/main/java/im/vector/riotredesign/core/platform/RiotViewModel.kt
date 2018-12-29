package im.vector.riotredesign.core.platform

import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxState

abstract class RiotViewModel<S : MvRxState>(initialState: S)
    : BaseMvRxViewModel<S>(initialState, debugMode = false)