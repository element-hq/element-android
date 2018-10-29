package im.vector.riotredesign.core.extensions


fun CharSequence.firstCharAsString(): String {
    return if (isNotEmpty()) this[0].toString() else ""
}