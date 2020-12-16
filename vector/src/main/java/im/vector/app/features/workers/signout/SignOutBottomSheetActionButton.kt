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
import android.widget.LinearLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ItemSignoutActionBinding
import im.vector.app.features.themes.ThemeUtils

class SignOutBottomSheetActionButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val views: ItemSignoutActionBinding

    var action: (() -> Unit)? = null

    var title: String? = null
        set(value) {
            field = value
            views.actionTitleText.setTextOrHide(value)
        }

    var leftIcon: Drawable? = null
        set(value) {
            field = value
            if (value == null) {
                views.actionIconImageView.isVisible = false
                views.actionIconImageView.setImageDrawable(null)
            } else {
                views.actionIconImageView.isVisible = true
                views.actionIconImageView.setImageDrawable(value)
            }
        }

    var tint: Int? = null
        set(value) {
            field = value
            views.actionIconImageView.imageTintList = value?.let { ColorStateList.valueOf(value) }
        }

    var textColor: Int? = null
        set(value) {
            field = value
            textColor?.let { views.actionTitleText.setTextColor(it) }
        }

    init {
        inflate(context, R.layout.item_signout_action, this)
        views = ItemSignoutActionBinding.bind(this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SignOutBottomSheetActionButton, 0, 0)
        title = typedArray.getString(R.styleable.SignOutBottomSheetActionButton_actionTitle) ?: ""
        leftIcon = typedArray.getDrawable(R.styleable.SignOutBottomSheetActionButton_leftIcon)
        tint = typedArray.getColor(R.styleable.SignOutBottomSheetActionButton_iconTint, ThemeUtils.getColor(context, android.R.attr.textColor))
        textColor = typedArray.getColor(R.styleable.SignOutBottomSheetActionButton_textColor, ThemeUtils.getColor(context, android.R.attr.textColor))

        typedArray.recycle()

        views.signedOutActionClickable.setOnClickListener {
            action?.invoke()
        }
    }
}
