package im.vector.matrix.android.api.permalinks

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.TextView
import im.vector.matrix.android.api.MatrixPatterns

object MatrixLinkify {

    /**
     * Find the matrix spans i.e matrix id , user id ... to display them as URL.
     *
     * @param spannable the text in which the matrix items has to be clickable.
     */
    fun addLinks(spannable: Spannable?, callback: MatrixPermalinkSpan.Callback?): Boolean {
        // sanity checks
        if (spannable.isNullOrEmpty()) {
            return false
        }
        val text = spannable.toString()
        var hasMatch = false
        for (index in MatrixPatterns.MATRIX_PATTERNS.indices) {
            val pattern = MatrixPatterns.MATRIX_PATTERNS[index]
            val matcher = pattern.matcher(spannable)
            while (matcher.find()) {
                hasMatch = true
                val startPos = matcher.start(0)
                if (startPos == 0 || text[startPos - 1] != '/') {
                    val endPos = matcher.end(0)
                    val url = text.substring(matcher.start(0), matcher.end(0))
                    val span = MatrixPermalinkSpan(url, callback)
                    spannable.setSpan(span, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return hasMatch
    }

    fun addLinks(textView: TextView, callback: MatrixPermalinkSpan.Callback?): Boolean {
        val text = textView.text
        if (text is Spannable) {
            if (addLinks(text, callback)) {
                addLinkMovementMethod(textView)
                return true
            }

            return false
        } else {
            val spannableString = SpannableString.valueOf(text)
            if (addLinks(spannableString, callback)) {
                addLinkMovementMethod(textView)
                textView.text = spannableString
                return true
            }
            return false
        }
    }


    private fun addLinkMovementMethod(textView: TextView) {
        val movementMethod = textView.movementMethod
        if (movementMethod == null || movementMethod !is LinkMovementMethod) {
            if (textView.linksClickable) {
                textView.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }


}