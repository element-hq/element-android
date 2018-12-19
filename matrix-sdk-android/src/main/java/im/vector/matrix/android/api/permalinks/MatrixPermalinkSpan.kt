package im.vector.matrix.android.api.permalinks

import android.text.style.ClickableSpan
import android.view.View

class MatrixPermalinkSpan(private val url: String,
                          private val callback: Callback? = null) : ClickableSpan() {

    interface Callback {
        fun onUrlClicked(url: String)
    }

    override fun onClick(widget: View) {
        callback?.onUrlClicked(url)
    }


}