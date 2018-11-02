package im.vector.riotredesign.core.platform

import android.support.v7.util.ListUpdateCallback

interface DefaultListUpdateCallback : ListUpdateCallback {

    override fun onChanged(position: Int, count: Int, tag: Any?) {
        //no-op
    }

    override fun onMoved(position: Int, count: Int) {
        //no-op
    }

    override fun onInserted(position: Int, count: Int) {
        //no-op
    }

    override fun onRemoved(position: Int, count: Int) {
        //no-op
    }
}