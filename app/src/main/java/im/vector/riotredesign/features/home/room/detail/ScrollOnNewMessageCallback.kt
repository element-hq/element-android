package im.vector.riotredesign.features.home.room.detail

import android.support.v7.widget.LinearLayoutManager
import im.vector.riotredesign.core.platform.DefaultListUpdateCallback

class ScrollOnNewMessageCallback(private val layoutManager: LinearLayoutManager) : DefaultListUpdateCallback {

    override fun onInserted(position: Int, count: Int) {
        if (layoutManager.findFirstVisibleItemPosition() == 0) {
            layoutManager.scrollToPosition(0)
        }
    }

}