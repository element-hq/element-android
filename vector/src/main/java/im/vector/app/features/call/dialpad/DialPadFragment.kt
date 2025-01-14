/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.dialpad

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.DialerKeyListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.android.dialer.dialpadview.DialpadView
import com.android.dialer.dialpadview.DigitsEditText
import com.android.dialer.dialpadview.R
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.themes.ThemeUtils

class DialPadFragment : Fragment(), TextWatcher {

    var callback: Callback? = null

    private lateinit var digits: DigitsEditText
    private var regionCode: String = DEFAULT_REGION_CODE
    private var formatAsYouType = true
    private var enableStar = true
    private var enablePound = true
    private var enablePlus = true
    private var cursorVisible = true
    private var enableDelete = true
    private var enableFabOk = true

    private lateinit var analyticsTracker: AnalyticsTracker

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val singletonEntryPoint = context.singletonEntryPoint()
        analyticsTracker = singletonEntryPoint.analyticsTracker()
    }

    override fun onResume() {
        super.onResume()
        analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.Dialpad))
    }

    @SuppressLint("WrongThread")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        initArgs(savedInstanceState)
        val view = inflater.inflate(R.layout.dialpad_fragment, container, false)
        view.setBackgroundColor(ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.backgroundColor))
        val dialpadView = view.findViewById<View>(R.id.dialpad_view) as DialpadView
        dialpadView.findViewById<View>(R.id.dialpad_key_voicemail).isVisible = false
        digits = dialpadView.digits as DigitsEditText
        digits.isCursorVisible = cursorVisible
        digits.inputType = InputType.TYPE_CLASS_PHONE
        digits.keyListener = DialerKeyListener.getInstance()
        digits.setTextColor(ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        digits.addTextChangedListener(PhoneNumberFormattingTextWatcher(if (formatAsYouType) regionCode else ""))
        digits.addTextChangedListener(this)
        dialpadView.findViewById<View>(R.id.zero).setOnClickListener { keyPressed(KeyEvent.KEYCODE_0, "0") }
        dialpadView.findViewById<View>(R.id.one).setOnClickListener { keyPressed(KeyEvent.KEYCODE_1, "1") }
        dialpadView.findViewById<View>(R.id.two).setOnClickListener { keyPressed(KeyEvent.KEYCODE_2, "2") }
        dialpadView.findViewById<View>(R.id.three).setOnClickListener { keyPressed(KeyEvent.KEYCODE_3, "3") }
        dialpadView.findViewById<View>(R.id.four).setOnClickListener { keyPressed(KeyEvent.KEYCODE_4, "4") }
        dialpadView.findViewById<View>(R.id.five).setOnClickListener { keyPressed(KeyEvent.KEYCODE_5, "5") }
        dialpadView.findViewById<View>(R.id.six).setOnClickListener { keyPressed(KeyEvent.KEYCODE_6, "6") }
        dialpadView.findViewById<View>(R.id.seven).setOnClickListener { keyPressed(KeyEvent.KEYCODE_7, "7") }
        dialpadView.findViewById<View>(R.id.eight).setOnClickListener { keyPressed(KeyEvent.KEYCODE_8, "8") }
        dialpadView.findViewById<View>(R.id.nine).setOnClickListener { keyPressed(KeyEvent.KEYCODE_9, "9") }
        if (enableStar) {
            dialpadView.findViewById<View>(R.id.star).setOnClickListener { keyPressed(KeyEvent.KEYCODE_STAR, "*") }
        } else {
            dialpadView.findViewById<View>(R.id.star).isVisible = false
        }
        if (enablePound) {
            dialpadView.findViewById<View>(R.id.pound).setOnClickListener { keyPressed(KeyEvent.KEYCODE_POUND, "#") }
        } else {
            dialpadView.findViewById<View>(R.id.pound).isVisible = false
        }
        if (enablePlus) {
            dialpadView.findViewById<View>(R.id.zero).setOnLongClickListener {
                keyPressed(KeyEvent.KEYCODE_PLUS, "+")
                true
            }
        }
        if (enableDelete) {
            dialpadView.deleteButton.setOnClickListener { keyPressed(KeyEvent.KEYCODE_DEL, null) }
            dialpadView.deleteButton.setOnLongClickListener {
                clear()
                true
            }
            val tintColor = ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
            ImageViewCompat.setImageTintList(dialpadView.deleteButton, ColorStateList.valueOf(tintColor))
        } else {
            dialpadView.deleteButton.isVisible = false
        }
        val fabOk = view.findViewById<View>(R.id.fab_ok)
        if (enableFabOk) {
            fabOk.setOnClickListener { onOkClicked() }
        } else {
            fabOk.isVisible = false
        }
        return view
    }

    private fun onOkClicked() {
        val rawInput = getRawInput()
        if (rawInput.isEmpty()) {
            val clipboard = requireContext().getSystemService<ClipboardManager>()
            val textToPaste = clipboard?.primaryClip?.getItemAt(0)?.text ?: return
            val formatted = formatNumber(textToPaste.toString())
            digits.setText(formatted)
            digits.setSelection(digits.text!!.length)
        } else {
            val formatted = digits.text.toString()
            callback?.onOkClicked(formatted, rawInput)
        }
    }

    fun getRawInput(): String {
        return PhoneNumberUtils.normalizeNumber(digits.text.toString())
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

    private fun keyPressed(keyCode: Int, digitString: String?) {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        // Disable cursor and enable it again after onKeyDown otherwise DigitsEditText force replacing cursor at the end
        digits.isCursorVisible = false
        digits.onKeyDown(keyCode, event)
        digits.isCursorVisible = cursorVisible
        digitString?.also {
            callback?.onDigitAppended(it)
        }
    }

    fun clear() {
        if (::digits.isInitialized) {
            digits.text = null
        }
    }

    private fun formatNumber(dialString: String): String {
        val networkPortion = PhoneNumberUtils.extractNetworkPortion(dialString)
        if (TextUtils.isEmpty(networkPortion)) {
            return ""
        }
        val number = PhoneNumberUtils.formatNumber(networkPortion, null, regionCode) ?: networkPortion
        // Also retrieve the post dial portion of the provided data, so that the entire dial string can be reconstituted
        val postDial = PhoneNumberUtils.extractPostDialPortion(dialString)
        return number + postDial
    }

    interface Callback {
        fun onDigitAppended(digit: String) = Unit
        fun onOkClicked(formatted: String?, raw: String?) = Unit
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

    // Text watcher

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Noop
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // Noop
    }

    override fun afterTextChanged(s: Editable) {
        if (s.isEmpty()) {
            digits.clearFocus()
        }
    }
}
