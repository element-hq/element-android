package im.vector.app.features.home.room.detail.timeline.helper

import androidx.recyclerview.widget.DiffUtil
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class InvalidateTimelineEventDiffUtilCallback(private val oldAndNewList: List<TimelineEvent>): DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldAndNewList.size
    }

    override fun getNewListSize(): Int {
        return oldAndNewList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // We want to 'fake' updated items with this class - updated means in this case, not completely invalidated.
        return oldItemPosition == newItemPosition
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // We want to 'fake' updated items with this class.
        return false
    }
}
