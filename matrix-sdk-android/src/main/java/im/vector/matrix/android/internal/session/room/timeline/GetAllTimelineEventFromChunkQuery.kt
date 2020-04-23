package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings

internal class GetAllTimelineEventFromChunkQuery(
        private val driver: SqlDriver,
        private val chunkId: Long,
        private val settings: TimelineSettings?,
        mapper: (SqlCursor) -> TimelineEvent
) : Query<TimelineEvent>(copyOnWriteList(), mapper) {

    override fun execute(): SqlCursor = driver.executeQuery(null, """
    |SELECT * FROM timelineWithRoot
    |WHERE chunk_id= $chunkId
    |${settings.computeFilterTypes("AND")}
    |${settings.computeFilterEdits("AND")}
    |ORDER BY display_index DESC
    """.trimMargin(), 0)

}
