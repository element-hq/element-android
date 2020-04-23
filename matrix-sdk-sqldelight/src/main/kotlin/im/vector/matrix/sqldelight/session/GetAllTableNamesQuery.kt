package im.vector.matrix.sqldelight.session

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList

internal class GetAllTableNamesQuery(
        private val driver: SqlDriver,
        mapper: (SqlCursor) -> String
) : Query<String>(copyOnWriteList(), mapper) {

    override fun execute(): SqlCursor = driver.executeQuery(hashCode(), """
    |SELECT name FROM sqlite_master 
    WHERE type = 'table' AND name NOT LIKE 'sqlite_%';
    """.trimMargin(), 0)

}
