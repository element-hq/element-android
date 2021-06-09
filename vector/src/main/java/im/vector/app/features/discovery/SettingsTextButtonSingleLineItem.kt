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
package im.vector.app.features.discovery

import android.widget.Button
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.google.android.material.switchmaterial.SwitchMaterial
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.attributes.ButtonStyle
import im.vector.app.core.epoxy.attributes.ButtonType
import im.vector.app.core.epoxy.attributes.IconMode
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass(layout = R.layout.item_settings_button_single_line)
abstract class SettingsTextButtonSingleLineItem : EpoxyModelWithHolder<SettingsTextButtonSingleLineItem.Holder>() {

    @EpoxyAttribute
    lateinit var colorProvider: ColorProvider

    @EpoxyAttribute
    lateinit var stringProvider: StringProvider

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    @StringRes
    var titleResId: Int? = null

    @EpoxyAttribute
    var iconMode: IconMode = IconMode.NONE

    @EpoxyAttribute
    var buttonTitle: String? = null

    @EpoxyAttribute
    @StringRes
    var buttonTitleId: Int? = null

    @EpoxyAttribute
    var buttonStyle: ButtonStyle = ButtonStyle.POSITIVE

    @EpoxyAttribute
    var buttonType: ButtonType = ButtonType.NORMAL

    @EpoxyAttribute
    var buttonIndeterminate: Boolean = false

    @EpoxyAttribute
    var checked: Boolean? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var buttonClickListener: ClickListener? = null

    @EpoxyAttribute
    var switchChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        if (titleResId != null) {
            holder.textView.setText(titleResId!!)
        } else {
            holder.textView.setTextOrHide(title, hideWhenBlank = false)
        }

        if (buttonTitleId != null) {
            holder.mainButton.setText(buttonTitleId!!)
        } else {
            holder.mainButton.setTextOrHide(buttonTitle)
        }

        if (buttonIndeterminate) {
            holder.progress.isVisible = true
            holder.mainButton.isInvisible = true
            holder.switchButton.isInvisible = true
            holder.switchButton.setOnCheckedChangeListener(null)
            holder.mainButton.setOnClickListener(null)
        } else {
            holder.progress.isVisible = false
            when (buttonType) {
                ButtonType.NO_BUTTON -> {
                    holder.mainButton.isVisible = false
                    holder.switchButton.isVisible = false
                }
                ButtonType.NORMAL    -> {
                    holder.mainButton.isVisible = true
                    holder.switchButton.isVisible = false
                    when (buttonStyle) {
                        ButtonStyle.POSITIVE    -> {
                            holder.mainButton.setTextColor(colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                        }
                        ButtonStyle.DESTRUCTIVE -> {
                            holder.mainButton.setTextColor(colorProvider.getColorFromAttribute(R.attr.colorError))
                        }
                    }.exhaustive
                    holder.mainButton.onClick(buttonClickListener)
                }
                ButtonType.SWITCH    -> {
                    holder.mainButton.isVisible = false
                    holder.switchButton.isVisible = true
                    // set to null before changing the state
                    holder.switchButton.setOnCheckedChangeListener(null)
                    checked?.let { holder.switchButton.isChecked = it }
                    holder.switchButton.setOnCheckedChangeListener(switchChangeListener)
                }
            }.exhaustive
        }

        when (iconMode) {
            IconMode.NONE  -> {
                holder.textView.setCompoundDrawables(null, null, null, null)
            }
            IconMode.INFO  -> {
                val errorColor = colorProvider.getColor(R.color.notification_accent_color)
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_notification_privacy_warning)?.apply {
                    ThemeUtils.tintDrawableWithColor(this, errorColor)
                    holder.textView.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
                }
            }
            IconMode.ERROR -> {
                val errorColor = colorProvider.getColorFromAttribute(R.attr.colorError)
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_notification_privacy_warning)?.apply {
                    ThemeUtils.tintDrawableWithColor(this, errorColor)
                    holder.textView.setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
                }
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.settings_item_text)
        val mainButton by bind<Button>(R.id.settings_item_button)
        val switchButton by bind<SwitchMaterial>(R.id.settings_item_switch)
        val progress by bind<ProgressBar>(R.id.settings_item_progress)
    }
}
