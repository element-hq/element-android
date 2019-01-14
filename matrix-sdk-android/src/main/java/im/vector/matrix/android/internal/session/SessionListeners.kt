package im.vector.matrix.android.internal.session

import im.vector.matrix.android.api.session.Session

internal class SessionListeners {

    private val listeners = ArrayList<Session.Listener>()

    fun addListener(listener: Session.Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Session.Listener) {
        listeners.remove(listener)
    }

}