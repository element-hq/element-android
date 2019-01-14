package im.vector.riotredesign.features.home.group

import arrow.core.Option
import com.jakewharton.rxrelay2.BehaviorRelay
import im.vector.matrix.android.api.session.group.model.GroupSummary
import io.reactivex.Observable

class SelectedGroupHolder {

    private val selectedGroupStream = BehaviorRelay.createDefault<Option<GroupSummary>>(Option.empty())

    fun setSelectedGroup(group: GroupSummary?) {
        val optionValue = Option.fromNullable(group)
        selectedGroupStream.accept(optionValue)
    }

    fun selectedGroup(): Observable<Option<GroupSummary>> {
        return selectedGroupStream.hide()
    }


}