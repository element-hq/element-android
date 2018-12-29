package im.vector.riotredesign.features.home.group

import arrow.core.Option
import im.vector.matrix.android.api.session.group.model.GroupSummary
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class SelectedGroupHolder {

    private val selectedGroupStream = BehaviorSubject.createDefault<Option<GroupSummary>>(Option.empty())

    fun setSelectedGroup(group: GroupSummary?) {
        val optionValue = Option.fromNullable(group)
        selectedGroupStream.onNext(optionValue)
    }

    fun selectedGroup(): Observable<Option<GroupSummary>> {
        return selectedGroupStream.hide()
    }


}