package im.vector.riotredesign.features.home.group

import im.vector.matrix.android.api.session.group.model.GroupSummary

sealed class GroupListActions {

    data class SelectGroup(val groupSummary: GroupSummary) : GroupListActions()

}