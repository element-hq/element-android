package im.vector.riotredesign.core.extensions

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import im.vector.riotredesign.R

fun EditText.setupAsSearch() {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            val clearIcon = if (editable?.isNotEmpty() == true) R.drawable.ic_clear_white else 0
            setCompoundDrawablesWithIntrinsicBounds(0, 0, clearIcon, 0)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    })

    maxLines = 1
    inputType = InputType.TYPE_CLASS_TEXT
    imeOptions = EditorInfo.IME_ACTION_SEARCH
    setOnEditorActionListener { _, actionId, event ->
        var consumed = false
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            hideKeyboard()
            consumed = true
        }
        consumed
    }

    setOnTouchListener(View.OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= (this.right - this.compoundPaddingRight)) {
                text = null
                return@OnTouchListener true
            }
        }
        return@OnTouchListener false
    })
}
