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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class EmojiRecyclerAdapter(val dataSource: EmojiDataSource? = null) :
        RecyclerView.Adapter<EmojiRecyclerAdapter.ViewHolder>() {

//    data class EmojiInfo(val stringValue: String)
//    data class SectionInfo(val sectionName: String)

    //val mockData: ArrayList<Pair<SectionInfo, ArrayList<EmojiInfo>>> = ArrayList()

    // val dataSource : EmojiDataSource? = null

    init {
//        val faces = ArrayList<EmojiInfo>()
//        for (i in 0..50) {
//            faces.add(EmojiInfo("😅"))
//        }
//        val animalsNature = ArrayList<EmojiInfo>()
//        for (i in 0..160) {
//            animalsNature.add(EmojiInfo("🐶"))
//        }
//        val foods = ArrayList<EmojiInfo>()
//        for (i in 0..150) {
//            foods.add(EmojiInfo("🍎"))
//        }
//
//        mockData.add(SectionInfo("Smiley & People") to faces)
//        mockData.add(SectionInfo("Animals & Nature") to animalsNature)
//        mockData.add(SectionInfo("Food & Drinks") to foods)
//        dataSource = EMp

    }

//    enum class ScrollState {
//        IDLE,
//        DRAGGING,
//        SETTLING,
//        UNKNWON
//    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        EmojiDrawView.configureTextPaint(recyclerView.context)

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

    @SuppressLint("NewApi")
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
                holder.bind(item)
            }
        }
        endTraceSession()

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
        abstract fun bind(s: String)
    }


    class EmojiViewHolder(itemView: View) : ViewHolder(itemView) {

        var emojiView: EmojiDrawView = itemView.findViewById(R.id.grid_item_emoji_text)


        override fun bind(s: String) {
            emojiView.emoji = s
        }
    }

    class SectionViewHolder(itemView: View) : ViewHolder(itemView) {

        var textView: TextView = itemView.findViewById(R.id.section_header_textview)

        override fun bind(s: String) {
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
    }
//    data class SectionsIndex(val dataSource: EmojiDataSource) {
//        var sectionsIndex: ArrayList<Int> = ArrayList()
//        var sectionsInfo: ArrayList<Pair<Int, Int>> = ArrayList()
//        var itemCount = 0
//
//        init {
//            var sectionOffset = 1
//            var lastItemInSection = 0
//            dataSource.rawData?.categories?.let {
//                for (category in it) {
//                    sectionsIndex.add(sectionOffset - 1)
//                    lastItemInSection = sectionOffset + category.emojis.size - 1
//                    sectionsInfo.add(sectionOffset to lastItemInSection)
//                    sectionOffset = lastItemInSection + 2
//                    itemCount += (1 + category.emojis.size)
//                }
//            }
//        }
//
//        fun getCount(): Int = this.itemCount
//
//        fun isSection(position: Int): Int? {
//            return sectionsIndex.indexOf(position)
//        }
//
//        fun getSectionForAbsoluteIndex(position: Int): Int {
//            for (i in sectionsIndex.size - 1 downTo 0) {
//                val sectionOffset = sectionsIndex[i]
//                if (position >= sectionOffset) {
//                    return i
//                }
//            }
//            return 0
//        }
//    }
}