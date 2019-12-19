package im.vector.riotx.features.crypto.verification

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.platform.VectorViewModel


data class VerificationBottomSheetViewState(
        val userId: String = "",
        val otherUserId: MatrixItem? = null,
        val verificationRequestEvent: Async<TimelineEvent> = Uninitialized
) : MvRxState

class VerificationBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: VerificationBottomSheetViewState,
                                                                   private val session: Session)
    : VectorViewModel<VerificationBottomSheetViewState, VerificationAction>(initialState) {

    init {
        withState {
            session.getUser(it.userId).let { user ->
                setState {
                    copy(otherUserId = user?.toMatrixItem())
                }
            }
        }
    }
    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationBottomSheetViewState): VerificationBottomSheetViewModel
    }


    companion object : MvRxViewModelFactory<VerificationBottomSheetViewModel, VerificationBottomSheetViewState> {

        override fun create(viewModelContext: ViewModelContext, state: VerificationBottomSheetViewState): VerificationBottomSheetViewModel? {
            val fragment: VerificationBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val userId: String = viewModelContext.args()
            return fragment.verificationViewModelFactory.create(VerificationBottomSheetViewState(userId))
        }
    }

    override fun handle(action: VerificationAction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
