package im.vector.matrix.android.internal.database.helper

import com.squareup.sqldelight.ColumnAdapter


private const val SEPARATOR = "__;__"

internal class ListOfStringColumnAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        return databaseValue.split(SEPARATOR).filter { it.isNotBlank() }
    }

    override fun encode(value: List<String>): String {
        return value.joinToString(SEPARATOR)
    }
}
