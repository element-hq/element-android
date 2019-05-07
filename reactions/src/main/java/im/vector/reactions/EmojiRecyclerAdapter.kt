/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.reactions

import android.os.Build
import android.os.Trace
import android.text.Layout
import android.text.StaticLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs


/**
 *
 * TODO: Configure Span using available width and emoji size
 * TODO: Search
 * TODO: Performances
 * TODO: Scroll to section - Find a way to snap section to the top
 */
class EmojiRecyclerAdapter(val dataSource: EmojiDataSource? = null) :
        RecyclerView.Adapter<EmojiRecyclerAdapter.ViewHolder>() {

    var interactionListener: InteractionListener? = null
    var mRecyclerView: RecyclerView? = null


    var currentFirstVisibleSection = 0

    enum class ScrollState {
        IDLE,
        DRAGGING,
        SETTLING,
        UNKNWON
    }

    private var scrollState = ScrollState.UNKNWON
    private var isFastScroll = false

    val toUpdateWhenNotBusy = ArrayList<Pair<String, EmojiViewHolder>>()


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.mRecyclerView = recyclerView

        val gridLayoutManager = GridLayoutManager(recyclerView.context, 8)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (isSection(position)) gridLayoutManager.spanCount else 1
            }
        }.apply {
            isSpanIndexCacheEnabled = true
        }
        recyclerView.layoutManager = gridLayoutManager

        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
        }

        recyclerView.setHasFixedSize(true)
        //Default is 5 but we have lots of views for emojis
        recyclerView.recycledViewPool
                .setMaxRecycledViews(R.layout.grid_item_emoji, 300)

        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.mRecyclerView = null
        recyclerView.removeOnScrollListener(scrollListener)
        staticLayoutCache.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun scrollToSection(section: Int) {
        if (section < 0 || section >= dataSource?.rawData?.categories?.size ?: 0) {
            //ignore
            return
        }
        //mRecyclerView?.smoothScrollToPosition(getSectionOffset(section) - 1)
        //TODO Snap section header to top
        mRecyclerView?.scrollToPosition(getSectionOffset(section) - 1)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        beginTraceSession("MyAdapter.onCreateViewHolder")
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(viewType, parent, false)
        val viewHolder = when (viewType) {
            R.layout.grid_section_header -> SectionViewHolder(itemView)
            else -> EmojiViewHolder(itemView)
        }
        endTraceSession()
        return viewHolder

    }

    override fun getItemViewType(position: Int): Int {
        beginTraceSession("MyAdapter.getItemViewType")
        if (isSection(position)) {
            return R.layout.grid_section_header
        }
        endTraceSession()
        return R.layout.grid_item_emoji
    }

    private fun isSection(position: Int): Boolean {
        dataSource?.rawData?.categories?.let { categories ->
            var sectionOffset = 1
            var lastItemInSection = 0
            for (category in categories) {
                lastItemInSection = sectionOffset + category.emojis.size - 1
                if (position == sectionOffset - 1) return true
                sectionOffset = lastItemInSection + 2
            }
        }
        return false
    }

    private fun getSectionForAbsoluteIndex(position: Int): Int {
        var sectionOffset = 1
        var lastItemInSection = 0
        var index = 0
        dataSource?.rawData?.categories?.let {
            for (category in it) {
                lastItemInSection = sectionOffset + category.emojis.size - 1
                if (position <= lastItemInSection) return index
                sectionOffset = lastItemInSection + 2
                index++
            }
        }
        return index
    }

    private fun getSectionOffset(section: Int): Int {
        //Todo cache this for fast access
        var sectionOffset = 1
        var lastItemInSection = 0
        dataSource?.rawData?.categories?.let {
            for ((index, category) in it.withIndex()) {
                lastItemInSection = sectionOffset + category.emojis.size - 1
                if (section == index) return sectionOffset
                sectionOffset = lastItemInSection + 2
            }
        }
        return sectionOffset
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        beginTraceSession("MyAdapter.onBindViewHolder")
        dataSource?.rawData?.categories?.let { categories ->
            val sectionNumber = getSectionForAbsoluteIndex(position)
            if (isSection(position)) {
                holder.bind(categories[sectionNumber].name)
            } else {
                val sectionMojis = categories[sectionNumber].emojis
                val sectionOffset = getSectionOffset(sectionNumber)
                val emoji = sectionMojis[position - sectionOffset]
                val item = dataSource!!.rawData!!.emojis[emoji]!!.emojiString()
                (holder as EmojiViewHolder).data = item
                if (scrollState != ScrollState.SETTLING || !isFastScroll) {
//                    Log.i("PERF","Bind with draw at position:$position")
                    holder.bind(item)
                } else {
//                    Log.i("PERF","Bind without draw at position:$position")
                    toUpdateWhenNotBusy.add(item to holder)
                    holder.bind(null)
                }
            }
        }
        endTraceSession()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        if (holder is EmojiViewHolder) {
            holder.data = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                toUpdateWhenNotBusy.removeIf { it.second == holder }
            } else {
                val index = toUpdateWhenNotBusy.indexOfFirst { it.second == holder }
                if (index != -1) {
                    toUpdateWhenNotBusy.removeAt(index)
                }
            }
        }
        super.onViewRecycled(holder)
    }


    override fun getItemCount(): Int {
        dataSource?.rawData?.categories?.let {
            var count = /*number of sections*/ it.size
            for (ad in it) {
                count += ad.emojis.size
            }
            return count
        } ?: kotlin.run { return 0 }
    }


    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(s: String?)
    }


    class EmojiViewHolder(itemView: View) : ViewHolder(itemView) {

        var emojiView: EmojiDrawView = itemView.findViewById(R.id.grid_item_emoji_text)
        val placeHolder: View = itemView.findViewById(R.id.grid_item_place_holder)

        var data: String? = null

        override fun bind(s: String?) {
            emojiView.emoji = s
            if (s != null) {
                emojiView.mLayout = getStaticLayoutForEmoji(s)
                placeHolder.visibility = View.GONE
//                emojiView.visibility = View.VISIBLE
            } else {
                emojiView.mLayout = null
                placeHolder.visibility = View.VISIBLE
//                emojiView.visibility = View.GONE
            }
        }
    }

    class SectionViewHolder(itemView: View) : ViewHolder(itemView) {

        var textView: TextView = itemView.findViewById(R.id.section_header_textview)

        override fun bind(s: String?) {
            textView.text = s
        }

    }

    companion object {
        fun endTraceSession() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Trace.endSection()
            }
        }

        fun beginTraceSession(sectionName: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Trace.beginSection(sectionName)
            }
        }

        val staticLayoutCache = HashMap<String, StaticLayout>()

        fun getStaticLayoutForEmoji(emoji: String): StaticLayout {
            var cachedLayout = staticLayoutCache[emoji]
            if (cachedLayout == null) {
                cachedLayout = StaticLayout(emoji, EmojiDrawView.tPaint, EmojiDrawView.emojiSize, Layout.Alignment.ALIGN_CENTER, 1f, 0f, true)
                staticLayoutCache[emoji] = cachedLayout!!
            }
            return cachedLayout!!
        }

    }

    interface InteractionListener {
        fun firstVisibleSectionChange(section: Int)
    }

    //privates

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            scrollState = when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> ScrollState.IDLE
                RecyclerView.SCROLL_STATE_SETTLING -> ScrollState.SETTLING
                RecyclerView.SCROLL_STATE_DRAGGING -> ScrollState.DRAGGING
                else -> ScrollState.UNKNWON
            }

            //TODO better
            if (scrollState == ScrollState.IDLE) {
                //
                val toUpdate = toUpdateWhenNotBusy.clone() as ArrayList<Pair<String, EmojiViewHolder>>
                toUpdateWhenNotBusy.clear()
                toUpdate.chunked(8).forEach {
                    recyclerView.post {
                        val transition = AutoTransition().apply {
                            duration = 150
                        }
                        for (pair in it) {
                            val holder = pair.second
                            if (pair.first == holder.data) {
                                TransitionManager.beginDelayedTransition(holder.itemView as FrameLayout, transition)
                                val data = holder.data
                                holder.bind(data)
                            }
                        }
                        toUpdateWhenNotBusy.clear()
                    }
                }

            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            //Log.i("SCROLL SPEED","scroll speed $dy")
            isFastScroll = abs(dy) > 50
            val visible = (recyclerView.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            GlobalScope.launch {
                val section = getSectionForAbsoluteIndex(visible)
                if (section != currentFirstVisibleSection) {
                    currentFirstVisibleSection = section
                    GlobalScope.launch(Dispatchers.Main) {
                        interactionListener?.firstVisibleSectionChange(currentFirstVisibleSection)
                    }
                }
            }
        }
    }
}