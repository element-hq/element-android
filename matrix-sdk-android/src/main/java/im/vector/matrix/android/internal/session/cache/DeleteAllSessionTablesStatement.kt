package im.vector.matrix.android.internal.session.cache

import com.squareup.sqldelight.db.SqlDriver
import im.vector.matrix.sqldelight.session.DeleteAllTableStatement
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

internal class DeleteAllSessionTablesStatement @Inject constructor(sqlDriver: SqlDriver,
                                                                   sessionDatabase: SessionDatabase) : DeleteAllTableStatement(sqlDriver, sessionDatabase)
