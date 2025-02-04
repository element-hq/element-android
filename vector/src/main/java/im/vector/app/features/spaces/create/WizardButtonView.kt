/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewSpaceTypeButtonBinding

class WizardButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        ConstraintLayout(context, attrs, defStyle) {

    private val views: ViewSpaceTypeButtonBinding

    var title: CharSequence? = null
        set(value) {
            if (value != title) {
                field = value
                views.title.setTextOrHide(value)
            }
        }

    var subTitle: String? = null
        set(value) {
            if (value != subTitle) {
                field = value
                views.subTitle.setTextOrHide(value)
            }
        }

    var icon: Drawable? = null
        set(value) {
            if (value != icon) {
                field = value
                views.buttonImageView.setImageDrawable(value)
            }
        }

    private var tint: Int? = null
        set(value) {
            field = value
            if (value != null) {
                views.buttonImageView.imageTintList = ColorStateList.valueOf(value)
            }
        }

//    var action: (() -> Unit)? = null

    init {
        val content = inflate(context, R.layout.view_space_type_button, this)
        views = ViewSpaceTypeButtonBinding.bind(content)

        views.subTitle.setTextOrHide(null)

        if (isInEditMode) {
            title = "Title"
            subTitle = "This is doing something"
        }

        context.withStyledAttributes(attrs, im.vector.lib.ui.styles.R.styleable.WizardButtonView) {
            title = getString(im.vector.lib.ui.styles.R.styleable.WizardButtonView_title)
            subTitle = getString(im.vector.lib.ui.styles.R.styleable.WizardButtonView_subTitle)
            icon = getDrawable(im.vector.lib.ui.styles.R.styleable.WizardButtonView_icon)
            tint = getColor(im.vector.lib.ui.styles.R.styleable.WizardButtonView_iconTint, -1)
                    .takeIf { it != -1 }
        }

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.foreground = getDrawable(context, outValue.resourceId)
        }

//        views.content.isClickable = true
//        views.content.isFocusable = true
//        views.content.setOnClickListener {
//            action?.invoke()
//        }
    }
}
