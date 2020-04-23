package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings

internal class GetPagedTimelineEventQuery(
        private val driver: SqlDriver,
        private val chunkId: Long,
        private val startDisplayIndex: Int,
        private val limit: Long,
        private val settings: TimelineSettings?,
        private val direction: Timeline.Direction,
        mapper: (SqlCursor) -> TimelineEvent
) : Query<TimelineEvent>(copyOnWriteList(), mapper) {


    override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT timelineWithRoot.* FROM timelineWithRoot
    |WHERE timelineWithRoot.chunk_id = $chunkId
    |${settings.computeFilterTypes("AND")}
    |${settings.computeFilterEdits("AND")}
    |${computeDirectionParams()}      
    |LIMIT $limit
    """.trimMargin(), 0)


    private fun computeDirectionParams(): String {
        return if (direction == Timeline.Direction.FORWARDS) {
            """
        |AND display_index >= $startDisplayIndex
        |ORDER BY display_index ASC"""
        } else {
            """
        |AND display_index <= $startDisplayIndex
        |ORDER BY display_index DESC """
        }
    }

}
