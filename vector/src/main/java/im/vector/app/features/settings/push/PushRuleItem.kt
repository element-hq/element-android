/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.push

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.notifications.toNotificationAction
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule

@EpoxyModelClass
abstract class PushRuleItem : VectorEpoxyModel<PushRuleItem.Holder>(R.layout.item_pushrule_raw) {

    @EpoxyAttribute
    lateinit var pushRule: PushRule

    // TODO i18n
    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        super.bind(holder)
        val context = holder.view.context
        if (pushRule.enabled) {
            holder.view.setBackgroundColor(Color.TRANSPARENT)
            holder.ruleId.text = pushRule.ruleId
        } else {
            holder.view.setBackgroundColor(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_header_background))
            holder.ruleId.text = "[Disabled] ${pushRule.ruleId}"
        }
        val actions = pushRule.getActions()
        if (actions.isEmpty()) {
            holder.actionIcon.isInvisible = true
        } else {
            holder.actionIcon.isVisible = true
            val notifAction = actions.toNotificationAction()

            if (notifAction.shouldNotify && !notifAction.soundName.isNullOrBlank()) {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_notify_noisy))
                holder.actionIcon.contentDescription = context.getString(CommonStrings.a11y_rule_notify_noisy)
            } else if (notifAction.shouldNotify) {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_notify_silent))
                holder.actionIcon.contentDescription = context.getString(CommonStrings.a11y_rule_notify_silent)
            } else {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_dont_notify))
                holder.actionIcon.contentDescription = context.getString(CommonStrings.a11y_rule_notify_off)
            }

            val description = StringBuffer()
            pushRule.conditions?.forEachIndexed { i, condition ->
                if (i > 0) description.append("\n")
                description.append(
                        condition.asExecutableCondition()?.technicalDescription()
                                ?: "UNSUPPORTED"
                )
            }
            if (description.isBlank()) {
                holder.description.text = "No Conditions"
            } else {
                holder.description.text = description
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val ruleId by bind<TextView>(R.id.pushRuleId)
        val description by bind<TextView>(R.id.pushRuleDescription)
        val actionIcon by bind<ImageView>(R.id.pushRuleActionIcon)
    }
}
