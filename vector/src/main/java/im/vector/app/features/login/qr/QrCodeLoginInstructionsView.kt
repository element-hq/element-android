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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
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
                R.styleable.QrCodeLoginInstructionsView,
                0,
                0
        ).use {
            setInstructions(it)
        }
    }

    private fun setInstructions(typedArray: TypedArray) {
        val instruction1 = typedArray.getString(R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction1)
        val instruction2 = typedArray.getString(R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction2)
        val instruction3 = typedArray.getString(R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction3)
        val instruction4 = typedArray.getString(R.styleable.QrCodeLoginInstructionsView_qrCodeLoginInstruction4)
        binding.instruction1TextView.setTextOrHide(instruction1)
        binding.instruction2TextView.setTextOrHide(instruction2)
        binding.instruction3TextView.setTextOrHide(instruction3)
        binding.instruction4TextView.setTextOrHide(instruction4)
    }
}
