package im.vector.riotredesign.features.home

import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotredesign.R

/**
 * Created by francois on 14/05/2018.
 */

class TimelineEventAdapter(private val callback: Callback? = null)
    : PagedListAdapter<Event, TimelineEventAdapter.ViewHolder>(EventDiffUtilCallback()) {


    private var currentList: List<Event>? = null

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

        fun bind(event: Event?) {
            if (event == null) {

            } else {
                titleView.text = event.toString()
            }
        }
    }

    override fun onCurrentListChanged(newList: PagedList<Event>?) {
        callback?.onEventsListChanged(currentList, newList)
        currentList = newList
    }

    interface Callback {
        fun onEventsListChanged(oldList: List<Event>?, newList: List<Event>?)
    }

}

