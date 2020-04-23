package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.sqldelight.session.Breadcrumbs
import im.vector.matrix.sqldelight.session.BreadcrumbsQueries

internal fun BreadcrumbsQueries.saveBreadcrumbs(breadcrumbs: List<String>) {
    deleteAll()
    breadcrumbs.forEachIndexed { index, roomId ->
        insert(Breadcrumbs.Impl(room_id = roomId, breadcrumb_index = index))
    }
}
