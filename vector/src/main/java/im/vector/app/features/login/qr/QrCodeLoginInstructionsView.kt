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

package im.vector.app.features.login.qr

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import im.vector.app.databinding.ViewQrCodeLoginInstructionsBinding

class QrCodeLoginInstructionsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewQrCodeLoginInstructionsBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.QrCodeLoginInstructionsView,
                0,
                0
        ).use {
            setInstructions(it)
        }
    }

    private fun setInstructions(typedArray: TypedArray) {
        val instruction1 = typedArray.getString(im.vector.lib.ui.styles.R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction1)
        val instruction2 = typedArray.getString(im.vector.lib.ui.styles.R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction2)
        val instruction3 = typedArray.getString(im.vector.lib.ui.styles.R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction3)
        setInstructions(
                listOf(
                        instruction1,
                        instruction2,
                        instruction3,
                )
        )
    }

    fun setInstructions(instructions: List<String?>?) {
        setInstruction(binding.instructions1Layout, binding.instruction1TextView, instructions?.getOrNull(0))
        setInstruction(binding.instructions2Layout, binding.instruction2TextView, instructions?.getOrNull(1))
        setInstruction(binding.instructions3Layout, binding.instruction3TextView, instructions?.getOrNull(2))
    }

    private fun setInstruction(instructionLayout: LinearLayout, instructionTextView: TextView, instruction: String?) {
        instruction?.let {
            instructionLayout.isVisible = true
            instructionTextView.text = instruction
        } ?: run {
            instructionLayout.isVisible = false
        }
    }
}
