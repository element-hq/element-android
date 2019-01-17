package im.vector.matrix.android.internal.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber

/**
 * To be attached to ProcessLifecycleOwner lifecycle
 */
internal class BackgroundDetectionObserver : LifecycleObserver {

    var isIsBackground: Boolean = false
        private set

    private
    val listeners = ArrayList<Listener>()

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Timber.d("App returning to foreground…")
        isIsBackground = false
        listeners.forEach { it.onMoveToForeground() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        Timber.d("App going to background…")
        isIsBackground = true
        listeners.forEach { it.onMoveToBackground() }
    }

    interface Listener {
        fun onMoveToForeground()
        fun onMoveToBackground()
    }

}