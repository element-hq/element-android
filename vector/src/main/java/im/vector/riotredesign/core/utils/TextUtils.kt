package im.vector.riotredesign.core.utils

import java.util.*

object TextUtils {

    private val suffixes = TreeMap<Int, String>().also {
        it.put(1000, "k")
        it.put(1000000, "M")
        it.put(1000000000, "G")
    }

    fun formatCountToShortDecimal(value: Int): String {
        try {
            if (value < 0) return "-" + formatCountToShortDecimal(-value)
            if (value < 1000) return value.toString() //deal with easy case

            val e = suffixes.floorEntry(value)
            val divideBy = e.key
            val suffix = e.value

            val truncated = value / (divideBy!! / 10) //the number part of the output times 10
            val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
            return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
        } catch (t: Throwable) {
            return value.toString()
        }
    }
}