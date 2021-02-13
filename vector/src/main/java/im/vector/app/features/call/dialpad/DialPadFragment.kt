/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.dialpad

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.android.dialer.dialpadview.DialpadView
import com.android.dialer.dialpadview.DigitsEditText
import com.android.dialer.dialpadview.R
import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import im.vector.app.features.themes.ThemeUtils

class DialPadFragment : Fragment() {

    var callback: Callback? = null

    private var digits: DigitsEditText? = null
    private var formatter: AsYouTypeFormatter? = null
    private var input = ""
    private var regionCode: String = DEFAULT_REGION_CODE
    private var formatAsYouType = true
    private var enableStar = true
    private var enablePound = true
    private var enablePlus = true
    private var cursorVisible = false
    private var enableDelete = true
    private var enableFabOk = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View {
        initArgs(savedInstanceState)
        val view = inflater.inflate(R.layout.dialpad_fragment, container, false)
        val dialpadView = view.findViewById<View>(R.id.dialpad_view) as DialpadView
        dialpadView.findViewById<View>(R.id.dialpad_key_voicemail).isVisible = false
        digits = dialpadView.digits as? DigitsEditText
        digits?.isCursorVisible = cursorVisible
        digits?.setTextColor(ThemeUtils.getColor(requireContext(), im.vector.app.R.attr.riotx_text_primary))
        dialpadView.findViewById<View>(R.id.zero).setOnClickListener { append('0') }
        if (enablePlus) {
            dialpadView.findViewById<View>(R.id.zero).setOnLongClickListener {
                append('+')
                true
            }
        }
        dialpadView.findViewById<View>(R.id.one).setOnClickListener { append('1') }
        dialpadView.findViewById<View>(R.id.two).setOnClickListener { append('2') }
        dialpadView.findViewById<View>(R.id.three).setOnClickListener { append('3') }
        dialpadView.findViewById<View>(R.id.four).setOnClickListener { append('4') }
        dialpadView.findViewById<View>(R.id.four).setOnClickListener { append('4') }
        dialpadView.findViewById<View>(R.id.five).setOnClickListener { append('5') }
        dialpadView.findViewById<View>(R.id.six).setOnClickListener { append('6') }
        dialpadView.findViewById<View>(R.id.seven).setOnClickListener { append('7') }
        dialpadView.findViewById<View>(R.id.eight).setOnClickListener { append('8') }
        dialpadView.findViewById<View>(R.id.nine).setOnClickListener { append('9') }
        if (enableStar) {
            dialpadView.findViewById<View>(R.id.star).setOnClickListener { append('*') }
        } else {
            dialpadView.findViewById<View>(R.id.star).isVisible = false
        }
        if (enablePound) {
            dialpadView.findViewById<View>(R.id.pound).setOnClickListener { append('#') }
        } else {
            dialpadView.findViewById<View>(R.id.pound).isVisible = false
        }
        if (enableDelete) {
            dialpadView.deleteButton.setOnClickListener { poll() }
            dialpadView.deleteButton.setOnLongClickListener {
                clear()
                true
            }
            val tintColor = ThemeUtils.getColor(requireContext(), im.vector.app.R.attr.riotx_text_secondary)
            ImageViewCompat.setImageTintList(dialpadView.deleteButton, ColorStateList.valueOf(tintColor))
        } else {
            dialpadView.deleteButton.isVisible = false
        }

        // if region code is null, no formatting is performed
        formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(if (formatAsYouType) regionCode else "")

        val fabOk = view.findViewById<View>(R.id.fab_ok)
        if (enableFabOk) {
            fabOk.setOnClickListener {
                callback?.onOkClicked(digits?.text.toString(), input)
            }
        } else {
            fabOk.isVisible = false
        }

        digits?.setOnTextContextMenuClickListener {
            val string = digits?.text.toString()
            clear()
            for (element in string) {
                append(element)
            }
        }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_REGION_CODE, regionCode)
        outState.putBoolean(EXTRA_FORMAT_AS_YOU_TYPE, formatAsYouType)
        outState.putBoolean(EXTRA_ENABLE_STAR, enableStar)
        outState.putBoolean(EXTRA_ENABLE_POUND, enablePound)
        outState.putBoolean(EXTRA_ENABLE_PLUS, enablePlus)
        outState.putBoolean(EXTRA_ENABLE_OK, enableFabOk)
        outState.putBoolean(EXTRA_ENABLE_DELETE, enableDelete)
        outState.putBoolean(EXTRA_CURSOR_VISIBLE, cursorVisible)
    }

    private fun initArgs(savedInstanceState: Bundle?) {
        val args = savedInstanceState ?: arguments
        if (args != null) {
            regionCode = args.getString(EXTRA_REGION_CODE, DEFAULT_REGION_CODE)
            formatAsYouType = args.getBoolean(EXTRA_FORMAT_AS_YOU_TYPE, formatAsYouType)
            enableStar = args.getBoolean(EXTRA_ENABLE_STAR, enableStar)
            enablePound = args.getBoolean(EXTRA_ENABLE_POUND, enablePound)
            enablePlus = args.getBoolean(EXTRA_ENABLE_PLUS, enablePlus)
            enableDelete = args.getBoolean(EXTRA_ENABLE_DELETE, enableDelete)
            enableFabOk = args.getBoolean(EXTRA_ENABLE_OK, enableFabOk)
            cursorVisible = args.getBoolean(EXTRA_CURSOR_VISIBLE, cursorVisible)
        }
    }

    private fun poll() {
        if (!input.isEmpty()) {
            input = input.substring(0, input.length - 1)
            formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(regionCode)
            if (formatAsYouType) {
                digits?.setText("")
                for (c in input.toCharArray()) {
                    digits?.setText(formatter?.inputDigit(c))
                }
            } else {
                digits?.setText(input)
            }
        }
    }

    private fun clear() {
        formatter?.clear()
        digits?.setText("")
        input = ""
    }

    private fun append(c: Char) {
        callback?.onDigitAppended(c.toString())
        input += c
        if (formatAsYouType) {
            digits?.setText(formatter?.inputDigit(c))
        } else {
            digits?.setText(input)
        }
    }

    interface Callback {
        fun onOkClicked(formatted: String?, raw: String?) = Unit
        fun onDigitAppended(digit: String) = Unit
    }

    companion object {
        const val EXTRA_REGION_CODE = "EXTRA_REGION_CODE"
        const val EXTRA_FORMAT_AS_YOU_TYPE = "EXTRA_FORMAT_AS_YOU_TYPE"
        const val EXTRA_ENABLE_STAR = "EXTRA_ENABLE_STAR"
        const val EXTRA_ENABLE_POUND = "EXTRA_ENABLE_POUND"
        const val EXTRA_ENABLE_PLUS = "EXTRA_ENABLE_PLUS"
        const val EXTRA_ENABLE_DELETE = "EXTRA_ENABLE_DELETE"
        const val EXTRA_ENABLE_OK = "EXTRA_ENABLE_OK"
        const val EXTRA_CURSOR_VISIBLE = "EXTRA_CURSOR_VISIBLE"

        private const val DEFAULT_REGION_CODE = "US"
    }
}
