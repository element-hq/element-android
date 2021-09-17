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

package im.vector.app.core.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setDrawableOrHide
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewBottomSheetActionButtonBinding
import im.vector.app.features.themes.ThemeUtils

class BottomSheetActionButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val views: ViewBottomSheetActionButtonBinding

    var title: String? = null
        set(value) {
            field = value
            views.bottomSheetActionTitle.setTextOrHide(value)
        }

    var subTitle: String? = null
        set(value) {
            field = value
            views.bottomSheetActionSubTitle.setTextOrHide(value)
        }

    var forceStartPadding: Boolean? = null
        set(value) {
            field = value
            if (leftIcon == null) {
                if (forceStartPadding == true) {
                    views.bottomSheetActionLeftIcon.isInvisible = true
                } else {
                    views.bottomSheetActionLeftIcon.isGone = true
                }
            }
        }

    var leftIcon: Drawable? = null
        set(value) {
            field = value
            if (value == null) {
                if (forceStartPadding == true) {
                    views.bottomSheetActionLeftIcon.isInvisible = true
                } else {
                    views.bottomSheetActionLeftIcon.isGone = true
                }
                views.bottomSheetActionLeftIcon.setImageDrawable(null)
            } else {
                views.bottomSheetActionLeftIcon.isVisible = true
                views.bottomSheetActionLeftIcon.setImageDrawable(value)
            }
        }

    var rightIcon: Drawable? = null
        set(value) {
            field = value
            views.bottomSheetActionIcon.setDrawableOrHide(value)
        }

    var tint: Int? = null
        set(value) {
            field = value
            views.bottomSheetActionLeftIcon.imageTintList = value?.let { ColorStateList.valueOf(value) }
        }

    var titleTextColor: Int? = null
        set(value) {
            field = value
            value?.let { views.bottomSheetActionTitle.setTextColor(it) }
        }

    var isBetaAction: Boolean? = null
        set(value) {
            field = value
            views.bottomSheetActionBeta.isVisible = field ?: false
        }

    init {
        inflate(context, R.layout.view_bottom_sheet_action_button, this)
        views = ViewBottomSheetActionButtonBinding.bind(this)

        context.withStyledAttributes(attrs, R.styleable.BottomSheetActionButton) {
            title = getString(R.styleable.BottomSheetActionButton_actionTitle) ?: ""
            subTitle = getString(R.styleable.BottomSheetActionButton_actionDescription) ?: ""
            forceStartPadding = getBoolean(R.styleable.BottomSheetActionButton_forceStartPadding, false)
            leftIcon = getDrawable(R.styleable.BottomSheetActionButton_leftIcon)

            rightIcon = getDrawable(R.styleable.BottomSheetActionButton_rightIcon)

            tint = getColor(R.styleable.BottomSheetActionButton_tint, ThemeUtils.getColor(context, android.R.attr.textColor))
            titleTextColor = getColor(R.styleable.BottomSheetActionButton_titleTextColor, ThemeUtils.getColor(context, R.attr.colorPrimary))

            isBetaAction = getBoolean(R.styleable.BottomSheetActionButton_betaAction, false)
        }
    }
}
