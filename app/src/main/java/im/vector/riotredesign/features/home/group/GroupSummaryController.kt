package im.vector.riotredesign.features.home.group

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.group.model.GroupSummary

class GroupSummaryController(private val callback: Callback? = null
) : TypedEpoxyController<GroupListViewState>() {

    override fun buildModels(viewState: GroupListViewState) {
        buildGroupModels(viewState.asyncGroups(), viewState.selectedGroup)
    }

    private fun buildGroupModels(summaries: List<GroupSummary>?, selected: GroupSummary?) {
        if (summaries.isNullOrEmpty()) {
            return
        }
        summaries.forEach { groupSummary ->
            val isSelected = groupSummary.groupId == selected?.groupId
            GroupSummaryItem(
                    groupName = groupSummary.displayName,
                    avatarUrl = groupSummary.avatarUrl,
                    isSelected = isSelected,
                    listener = { callback?.onGroupSelected(groupSummary) }
            )
                    .id(groupSummary.groupId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onGroupSelected(groupSummary: GroupSummary)
    }

}
