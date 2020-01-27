package im.vector.riotx.features.roommemberprofile.devices

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider

data class DeviceListViewState(
        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Loading()
) : MvRxState

class DeviceListBottomSheetViewModel @AssistedInject constructor(@Assisted private val initialState: DeviceListViewState,
                                                                 @Assisted private val userId: String,
                                                                 private val stringProvider: StringProvider,
                                                                 private val session: Session) : VectorViewModel<DeviceListViewState, EmptyAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DeviceListViewState, userId: String): DeviceListBottomSheetViewModel
    }

    init {
        session.rx().liveUserCryptoDevices(userId)
                .execute {
                    copy(cryptoDevices = it)
                }
    }

    override fun handle(action: EmptyAction) {}

    companion object : MvRxViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> {
        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DeviceListViewState): DeviceListBottomSheetViewModel? {
            val fragment: DeviceListBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val userId = viewModelContext.args<String>()
            return fragment.viewModelFactory.create(state, userId)
        }
    }
}
