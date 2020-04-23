package im.vector.matrix.android.internal.database.helper

fun List<Any>.toInParams(): String{
    return if (isEmpty()) {
        "()"
    } else {
        val list = this
        buildString {
            append("(")
            append("\"${list.first()}\"")
            for (value in list.drop(1)) {
                append(",")
                append("\"$value\"")
            }
            append(')')
        }
    }
}
