package im.vector.riotredesign.features.home

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.home.room.list.RoomSelectionRepository
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get

class EmptyState : MvRxState

class HomeActivityViewModel(state: EmptyState,
                            private val session: Session,
                            roomSelectionRepository: RoomSelectionRepository
) : RiotViewModel<EmptyState>(state) {

    companion object : MvRxViewModelFactory<EmptyState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: EmptyState): HomeActivityViewModel {
            val session = Matrix.getInstance().currentSession
            val roomSelectionRepository = activity.get<RoomSelectionRepository>()
            return HomeActivityViewModel(state, session, roomSelectionRepository)
        }
    }

    private val _openRoomLiveData = MutableLiveData<LiveEvent<String>>()
    val openRoomLiveData: LiveData<LiveEvent<String>>
        get() = _openRoomLiveData

    init {
        val lastSelectedRoom = roomSelectionRepository.lastSelectedRoom()
        if (lastSelectedRoom == null) {
            getTheFirstRoomWhenAvailable()
        } else {
            _openRoomLiveData.postValue(LiveEvent(lastSelectedRoom))
        }
    }

    private fun getTheFirstRoomWhenAvailable() {
        session.rx().liveRoomSummaries()
                .filter { it.isNotEmpty() }
                .first(emptyList())
                .subscribeBy {
                    val firstRoom = it.firstOrNull()
                    if (firstRoom != null) {
                        _openRoomLiveData.postValue(LiveEvent(firstRoom.roomId))
                    }
                }
                .disposeOnClear()
    }


}