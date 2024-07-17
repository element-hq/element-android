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
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewQrCodeLoginHeaderBinding

class QrCodeLoginHeaderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewQrCodeLoginHeaderBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.QrCodeLoginHeaderView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
            setImage(it)
        }
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(im.vector.lib.ui.styles.R.styleable.QrCodeLoginHeaderView_qrCodeLoginHeaderTitle)
        setTitle(title)
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.QrCodeLoginHeaderView_qrCodeLoginHeaderDescription)
        setDescription(description)
    }

    private fun setImage(typedArray: TypedArray) {
        val imageResource = typedArray.getResourceId(im.vector.lib.ui.styles.R.styleable.QrCodeLoginHeaderView_qrCodeLoginHeaderImageResource, 0)
        val backgroundTint = typedArray.getColor(im.vector.lib.ui.styles.R.styleable.QrCodeLoginHeaderView_qrCodeLoginHeaderImageBackgroundTint, 0)
        setImage(imageResource, backgroundTint)
    }

    fun setTitle(title: String?) {
        binding.qrCodeLoginHeaderTitleTextView.setTextOrHide(title)
    }

    fun setDescription(description: String?) {
        binding.qrCodeLoginHeaderDescriptionTextView.setTextOrHide(description)
    }

    fun setImage(imageResource: Int, backgroundTintColor: Int) {
        binding.qrCodeLoginHeaderImageView.setImageResource(imageResource)
        binding.qrCodeLoginHeaderImageView.backgroundTintList = ColorStateList.valueOf(backgroundTintColor)
    }
}
