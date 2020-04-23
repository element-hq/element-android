package im.vector.matrix.sqldelight.session

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver

open class DeleteAllTableStatement(private val driver: SqlDriver,
                                   private val transacter: Transacter) {

    fun execute() {
        val tableNames = GetAllTableNamesQuery(driver) {
            it.getString(0)!!
        }.executeAsList()
        transacter.transaction {
            tableNames.forEach {
                driver.execute(null, "DELETE FROM $it;", 0)
            }
        }
    }
}
