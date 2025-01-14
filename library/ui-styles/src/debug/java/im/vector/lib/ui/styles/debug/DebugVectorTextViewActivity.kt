/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.ui.styles.debug

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import im.vector.lib.ui.styles.databinding.ActivityDebugTextViewBinding

// Rendering is not the same with VectorBaseActivity
abstract class DebugVectorTextViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val views = ActivityDebugTextViewBinding.inflate(layoutInflater)
        setContentView(views.root)

        views.debugShowPassword.setOnClickListener {
            views.debugTextInputEditText.showPassword(true)
        }
        views.debugHidePassword.setOnClickListener {
            views.debugTextInputEditText.showPassword(false)
        }
    }

    private fun EditText.showPassword(visible: Boolean) {
        if (visible) {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}
