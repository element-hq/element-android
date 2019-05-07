package im.vector.riotredesign.features.home.room.detail.timeline.action

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get
import timber.log.Timber


data class MessageActionState(
        val userId: String,
        val senderName: String,
        val messageBody: String,
        val senderAvatarPath: String? = null)
    : MvRxState


class MessageActionsViewModel(initialState: MessageActionState) : VectorViewModel<MessageActionState>(initialState) {

    companion object : MvRxViewModelFactory<MessageActionsViewModel, MessageActionState> {

//        override fun create(viewModelContext: ViewModelContext, state: MessageActionState): MessageActionsViewModel? {
//            //val currentSession = viewModelContext.activity.get<Session>()
//            return MessageActionsViewModel(state/*,currentSession*/)
//        }

        override fun initialState(viewModelContext: ViewModelContext): MessageActionState? {
            val currentSession = viewModelContext.activity.get<Session>()
            val parcel = viewModelContext.args as MessageActionsBottomSheet.ParcelableArgs


            val event = currentSession.getRoom(parcel.roomId)?.getTimeLineEvent(parcel.eventId)
            return if (event != null) {
                val messageContent: MessageContent? = event.root.content.toModel()

                MessageActionState(
                        event.root.sender ?: "",
                        parcel.informationData.memberName.toString(),
                        messageContent?.body ?: "",
                        currentSession.contentUrlResolver().resolveFullSize(parcel.informationData.avatarUrl)
                )
            } else {
                //can this happen?
                Timber.e("Failed to retrieve event")
                null
            }
        }
    }
}