package im.vector.matrix.sqldelight.session

import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.*

interface SessionTests {

    fun sessionDatabase(): SessionDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties())
        SessionDatabase.Schema.create(driver)
        val database = SessionDatabase(driver,
                                       groupSummaryEntityAdapter = GroupSummaryEntity.Adapter(EnumColumnAdapter()),
                                       roomMemberSummaryEntityAdapter = RoomMemberSummaryEntity.Adapter(EnumColumnAdapter()),
                                       roomSummaryEntityAdapter = RoomSummaryEntity.Adapter(EnumColumnAdapter())

        )
        return database
    }
}
