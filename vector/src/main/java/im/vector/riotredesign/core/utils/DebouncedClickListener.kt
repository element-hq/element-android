package im.vector.riotredesign.core.utils

import android.view.View
import java.util.*


/**
 * Simple Debounced OnClickListener
 * Safe to use in different views
 */
class DebouncedClickListener(val original: View.OnClickListener, private val minimumInterval: Long = 400) : View.OnClickListener {
    private val lastClickMap = WeakHashMap<View, Long>()

    override fun onClick(clickedView: View) {
        val previousClickTimestamp = lastClickMap[clickedView]
        val currentTimestamp = System.currentTimeMillis()

        lastClickMap[clickedView] = currentTimestamp

        if (previousClickTimestamp == null || currentTimestamp - previousClickTimestamp.toLong() > minimumInterval) {
            original.onClick(clickedView)
        }
    }
}