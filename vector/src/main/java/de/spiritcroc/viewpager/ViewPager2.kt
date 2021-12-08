package de.spiritcroc.viewpager

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import timber.log.Timber
import java.lang.Exception

// Mainly taken from https://stackoverflow.com/a/63455547
fun ViewPager2.reduceDragSensitivity(factor: Int) {
    try {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
       val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * factor)
        Timber.i("Reduced viewpager drag sensitivity from $touchSlop by factor $factor")
    } catch (e: Exception) {
        Timber.e("Cannot reduce viewpager drag sensitivity: $e")
    }
}
