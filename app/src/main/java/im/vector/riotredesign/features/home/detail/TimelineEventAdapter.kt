package im.vector.riotredesign.features.home.detail

import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.riotredesign.R

/**
 * Created by francois on 14/05/2018.
 */

class TimelineEventAdapter(private val callback: Callback? = null)
    : PagedListAdapter<EnrichedEvent, TimelineEventAdapter.ViewHolder>(EventDiffUtilCallback()) {


    private var currentList: List<EnrichedEvent>? = null

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val event = getItem(position)
        viewHolder.bind(event)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val titleView = view.findViewById<TextView>(R.id.titleView)!!

        fun bind(event: EnrichedEvent?) {
            if (event == null) {
                titleView.text = null
            } else if (event.root.type == EventType.MESSAGE) {
                val messageContent = event.root.content<MessageContent>()
                val roomMember = event.getMetaEvents(EventType.STATE_ROOM_MEMBER).firstOrNull()?.content<RoomMember>()
                if (messageContent == null || roomMember == null) {
                    titleView.text = null
                } else {
                    val text = "${roomMember.displayName} : ${messageContent.body}"
                    titleView.text = text
                }
            } else {
                titleView.text = event.root.toString()
            }
        }
    }

    override fun onCurrentListChanged(newList: PagedList<EnrichedEvent>?) {
        callback?.onEventsListChanged(currentList, newList)
        currentList = newList
    }

    interface Callback {
        fun onEventsListChanged(oldList: List<EnrichedEvent>?, newList: List<EnrichedEvent>?)
    }

}

