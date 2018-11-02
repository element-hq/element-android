package im.vector.riotredesign.features.home.room.detail.timeline

import im.vector.riotredesign.core.resources.LocaleProvider
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class TimelineDateFormatter(private val localeProvider: LocaleProvider) {

    fun formatMessageHour(localDateTime: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("H:mm", localeProvider.current()).format(localDateTime)
    }

    fun formatMessageDay(localDateTime: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("EEE d MMM", localeProvider.current()).format(localDateTime)
    }

}