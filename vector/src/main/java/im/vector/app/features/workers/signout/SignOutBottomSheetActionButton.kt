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
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewSignOutBottomSheetActionButtonBinding
import im.vector.app.features.themes.ThemeUtils

class SignOutBottomSheetActionButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val views: ViewSignOutBottomSheetActionButtonBinding

    var action: (() -> Unit)? = null

    private var title: String? = null
        set(value) {
            field = value
            views.actionTitleText.setTextOrHide(value)
        }

    private var leftIcon: Drawable? = null
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

    private var tint: Int? = null
        set(value) {
            field = value
            views.actionIconImageView.imageTintList = value?.let { ColorStateList.valueOf(value) }
        }

    private var textColor: Int? = null
        set(value) {
            field = value
            value?.let { views.actionTitleText.setTextColor(it) }
        }

    init {
        inflate(context, R.layout.view_sign_out_bottom_sheet_action_button, this)
        views = ViewSignOutBottomSheetActionButtonBinding.bind(this)

        context.withStyledAttributes(attrs, R.styleable.SignOutBottomSheetActionButton) {
            title = getString(R.styleable.SignOutBottomSheetActionButton_actionTitle) ?: ""
            leftIcon = getDrawable(R.styleable.SignOutBottomSheetActionButton_leftIcon)
            tint = getColor(R.styleable.SignOutBottomSheetActionButton_iconTint, ThemeUtils.getColor(context, R.attr.vctr_content_primary))
            textColor = getColor(R.styleable.SignOutBottomSheetActionButton_textColor, ThemeUtils.getColor(context, R.attr.vctr_content_primary))
        }

        views.signedOutActionClickable.setOnClickListener {
            action?.invoke()
        }
    }
}
