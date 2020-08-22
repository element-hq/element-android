/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.workers.signout

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils

class SignoutBottomSheetActionButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    @BindView(R.id.actionTitleText)
    lateinit var actionTextView: TextView

    @BindView(R.id.actionIconImageView)
    lateinit var iconImageView: ImageView

    @BindView(R.id.signedOutActionClickable)
    lateinit var clickableZone: View

    var action: (() -> Unit)? = null

    var title: String? = null
        set(value) {
            field = value
            actionTextView.setTextOrHide(value)
        }

    var leftIcon: Drawable? = null
        set(value) {
            field = value
            if (value == null) {
                iconImageView.isVisible = false
                iconImageView.setImageDrawable(null)
            } else {
                iconImageView.isVisible = true
                iconImageView.setImageDrawable(value)
            }
        }

    var tint: Int? = null
        set(value) {
            field = value
            iconImageView.imageTintList = value?.let { ColorStateList.valueOf(value) }
        }

    var textColor: Int? = null
        set(value) {
            field = value
            textColor?.let { actionTextView.setTextColor(it) }
        }

    init {
        inflate(context, R.layout.item_signout_action, this)
        ButterKnife.bind(this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SignoutBottomSheetActionButton, 0, 0)
        title = typedArray.getString(R.styleable.SignoutBottomSheetActionButton_actionTitle) ?: ""
        leftIcon = typedArray.getDrawable(R.styleable.SignoutBottomSheetActionButton_leftIcon)
        tint = typedArray.getColor(R.styleable.SignoutBottomSheetActionButton_iconTint, ThemeUtils.getColor(context, android.R.attr.textColor))
        textColor = typedArray.getColor(R.styleable.SignoutBottomSheetActionButton_textColor, ThemeUtils.getColor(context, android.R.attr.textColor))

        typedArray.recycle()

        clickableZone.setOnClickListener {
            action?.invoke()
        }
    }
}
