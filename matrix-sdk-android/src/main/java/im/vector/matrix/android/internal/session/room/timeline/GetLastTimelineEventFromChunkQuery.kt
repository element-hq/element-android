package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings

internal class GetLastTimelineEventFromChunkQuery(
        private val driver: SqlDriver,
        private val chunkId: Long,
        private val settings: TimelineSettings,
        private val direction: Timeline.Direction,
        mapper: (SqlCursor) -> TimelineEvent
) : Query<TimelineEvent>(copyOnWriteList(), mapper) {


    override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT timelineWithRoot.* FROM timelineWithRoot
    |WHERE timelineWithRoot.chunk_id = $chunkId
    |${settings.computeFilterTypes("AND")}
    |${settings.computeFilterEdits("AND")}
    |${computeDirection()}
    |LIMIT 1      
    """.trimMargin(), 0)


    private fun computeDirection(): String {
        return if (direction == Timeline.Direction.FORWARDS) {
            """
        |ORDER BY display_index DESC"""
        } else {
            """
        |ORDER BY display_index ASC """
        }
    }

}
