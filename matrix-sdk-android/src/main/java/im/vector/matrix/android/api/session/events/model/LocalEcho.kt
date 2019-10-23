package im.vector.matrix.android.api.session.events.model

object LocalEcho {

    const val PREFIX = "local."

    fun isLocalEchoId(eventId: String): Boolean = eventId.startsWith(PREFIX)
}
