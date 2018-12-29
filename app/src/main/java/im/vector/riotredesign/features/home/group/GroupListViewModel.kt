package im.vector.riotredesign.features.home.group

import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import org.koin.android.ext.android.get

class GroupListViewModel(initialState: GroupListViewState,
                         private val selectedGroupHolder: SelectedGroupHolder,
                         private val session: Session
) : RiotViewModel<GroupListViewState>(initialState) {

    companion object : MvRxViewModelFactory<GroupListViewState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: GroupListViewState): GroupListViewModel {
            val currentSession = Matrix.getInstance().currentSession
            val selectedGroupHolder = activity.get<SelectedGroupHolder>()
            return GroupListViewModel(state, selectedGroupHolder, currentSession)
        }
    }

    init {
        observeGroupSummaries()
        observeState()
    }

    private fun observeState() {
        subscribe {
            selectedGroupHolder.setSelectedGroup(it.selectedGroup)
        }
    }

    fun accept(action: GroupListActions) {
        when (action) {
            is GroupListActions.SelectGroup -> handleSelectGroup(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectGroup(action: GroupListActions.SelectGroup) = withState { state ->
        if (state.selectedGroup?.groupId != action.groupSummary.groupId) {
            setState { copy(selectedGroup = action.groupSummary) }
        } else {
            setState { copy(selectedGroup = null) }
        }
    }


    private fun observeGroupSummaries() {
        session
                .rx().liveGroupSummaries()
                .execute { async ->
                    copy(asyncGroups = async)
                }
    }


}