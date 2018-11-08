package im.vector.matrix.android.internal.network

import android.content.Context
import com.novoda.merlin.Merlin
import com.novoda.merlin.MerlinsBeard
import com.novoda.merlin.registerable.connection.Connectable

internal class NetworkConnectivityChecker(context: Context) {

    private val merlin = Merlin.Builder().withConnectableCallbacks().build(context)
    private val merlinsBeard = MerlinsBeard.from(context)

    private val listeners = ArrayList<Listener>()

    fun register(listener: Listener) {
        if (listeners.isEmpty()) {
            merlin.bind()
        }
        listeners.add(listener)
        val connectable = Connectable {
            if (listeners.contains(listener)) {
                listener.onConnect()
            }
        }
        merlin.registerConnectable(connectable)
    }

    fun unregister(listener: Listener) {
        if (listeners.remove(listener) && listeners.isEmpty()) {
            merlin.unbind()
        }
    }

    fun isConnected(): Boolean {
        return merlinsBeard.isConnected
    }

    interface Listener {
        fun onConnect()
    }


}

