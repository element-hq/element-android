package im.vector.riotredesign.features.home.room.detail.timeline.helper

import androidx.recyclerview.widget.RecyclerView

class TimelineMediaSizeProvider {

    lateinit var recyclerView: RecyclerView
    private var cachedSize: Pair<Int, Int>? = null

    fun getMaxSize(): Pair<Int, Int> {
        return cachedSize ?: computeMaxSize().also { cachedSize = it }
    }

    private fun computeMaxSize(): Pair<Int, Int> {
        val width = recyclerView.width
        val height = recyclerView.height
        val maxImageWidth: Int
        val maxImageHeight: Int
        // landscape / portrait
        if (width < height) {
            maxImageWidth = Math.round(width * 0.7f)
            maxImageHeight = Math.round(height * 0.5f)
        } else {
            maxImageWidth = Math.round(width * 0.5f)
            maxImageHeight = Math.round(height * 0.7f)
        }
        return Pair(maxImageWidth, maxImageHeight)
    }


}