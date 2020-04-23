package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.database.helper.toInParams
import im.vector.matrix.android.internal.database.query.FilterContent

internal fun TimelineSettings?.computeFilterEdits(prefix: String): String {
    return if (this == null || !filterEdits) {
       ""
    } else {
        """
            |$prefix
            |timelineWithRoot.content NOT LIKE ${FilterContent.EDIT_TYPE}
            |AND
            |timelineWithRoot.content NOT LIKE ${FilterContent.RESPONSE_TYPE}
            """
    }
}

internal fun TimelineSettings?.computeFilterTypes(prefix: String): String {
    return if (this== null || !filterTypes) {
        ""
    } else {
        val types = allowedTypes.toInParams()
        """$prefix timelineWithRoot.type IN $types"""
    }
}
