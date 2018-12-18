package im.vector.matrix.android.api.permalinks

import android.text.Spannable
import android.text.SpannableStringBuilder

object MatrixUrlLinkify {

    /**
     * Find the matrix spans i.e matrix id , user id ... to display them as URL.
     *
     * @param spannableStringBuilder the text in which the matrix items has to be clickable.
     */
    fun addLinks(spannableStringBuilder: SpannableStringBuilder, callback: MatrixURLSpan.Callback?) {
        // sanity checks
        if (spannableStringBuilder.isEmpty()) {
            return
        }
        val text = spannableStringBuilder.toString()
        for (index in MatrixPatterns.MATRIX_PATTERNS.indices) {
            val pattern = MatrixPatterns.MATRIX_PATTERNS[index]
            val matcher = pattern.matcher(spannableStringBuilder)
            while (matcher.find()) {
                val startPos = matcher.start(0)
                if (startPos == 0 || text[startPos - 1] != '/') {
                    val endPos = matcher.end(0)
                    val url = text.substring(matcher.start(0), matcher.end(0))
                    val span = MatrixURLSpan(url, callback)
                    spannableStringBuilder.setSpan(span, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }


}