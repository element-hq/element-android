package im.vector.riotredesign.core.extensions

import im.vector.matrix.android.api.session.events.model.Event
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId


fun Event.localDateTime(): LocalDateTime {
    val instant = Instant.ofEpochMilli(originServerTs ?: 0)
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}