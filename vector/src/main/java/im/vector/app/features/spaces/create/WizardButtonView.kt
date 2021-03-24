/*
 * Copyright (c) 2021 New Vector Ltd
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

class WizardButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private val views: ViewSpaceTypeButtonBinding

    var title: String? = null
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

        context.withStyledAttributes(attrs, R.styleable.WizardButtonView) {
            title = getString(R.styleable.WizardButtonView_title)
            subTitle = getString(R.styleable.WizardButtonView_subTitle)
            icon = getDrawable(R.styleable.WizardButtonView_icon)
            tint = getColor(R.styleable.WizardButtonView_iconTint, -1)
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
