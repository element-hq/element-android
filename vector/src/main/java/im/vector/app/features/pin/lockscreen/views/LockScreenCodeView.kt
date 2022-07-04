/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.pin.lockscreen.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.view.setMargins
import im.vector.app.R

/**
 * Custom view representing the entered digits of a PIN code screen.
 */
class LockScreenCodeView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val code: MutableList<Char> = mutableListOf()

    /**
     * Number of digits entered.
     */
    val enteredDigits: Int get() = code.size

    /**
     * Callback called when the PIN code has been completely entered.
     */
    var onCodeCompleted: CodeCompletedListener? = null

    var codeLength: Int = 0
        set(value) {
            if (value == field) return
            field = value
            setupCodeViews()
            code.clear()
        }

    init {
        isSaveEnabled = true

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).also { it.gravity = Gravity.CENTER_HORIZONTAL }
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    @SuppressLint("InflateParams")
    private fun setupCodeViews() {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        repeat(codeLength) { index ->
            val checkBox = inflater.inflate(R.layout.view_code_checkbox, null) as CheckBox
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            val margin = resources.getDimensionPixelSize(R.dimen.lockscreen_code_margin)
            params.setMargins(margin)
            checkBox.layoutParams = params
            checkBox.isChecked = code.size > index
            addView(checkBox)
        }
    }

    private fun getCodeView(index: Int): CheckBox? = getChildAt(index) as? CheckBox

    /**
     * Adds a new [character] to the PIN code. Once it reaches the [codeLength] needed it will invoke the [onCodeCompleted] callback.
     */
    fun onCharInput(character: Char): Int {
        if (code.size == codeLength) return code.size
        getCodeView(code.size)?.toggle()
        code.add(character)
        if (code.size == codeLength) {
            onCodeCompleted?.onCodeCompleted(String(code.toCharArray()))
        }
        return code.size
    }

    /**
     * Deletes the last digit in the PIN code if possible.
     */
    fun deleteLast(): Int {
        if (code.size == 0) return code.size
        code.removeLast()
        getCodeView(code.size)?.toggle()
        return code.size
    }

    /**
     * Removes all digits in the PIN code.
     */
    fun clearCode() {
        code.clear()
        repeat(codeLength) { getCodeView(it)?.isChecked = false }
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()!!).also {
            it.code = code
            it.codeLength = codeLength
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            codeLength = state.codeLength
            code.addAll(state.code)
        }
        super.onRestoreInstanceState(state)
        setupCodeViews()
    }

    /**
     * Used to listen to when [LockScreenCodeView] receives a whole PIN code.
     */
    fun interface CodeCompletedListener {
        fun onCodeCompleted(code: String)
    }

    internal class SavedState : BaseSavedState {
        var code: MutableList<Char> = mutableListOf()
        var codeLength: Int = 0

        constructor(source: Parcel) : super(source) {
            source.readList(code, null)
            codeLength = source.readInt()
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeList(code)
            out.writeInt(codeLength)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
