/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.features.notifications.toNotificationAction
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.pushrules.getActions
import org.matrix.android.sdk.api.pushrules.rest.PushRule

@EpoxyModelClass(layout = R.layout.item_pushrule_raw)
abstract class PushRuleItem : EpoxyModelWithHolder<PushRuleItem.Holder>() {

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
            holder.view.setBackgroundColor(ThemeUtils.getColor(context, R.attr.vctr_header_background))
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
                holder.actionIcon.contentDescription = context.getString(R.string.a11y_rule_notify_noisy)
            } else if (notifAction.shouldNotify) {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_notify_silent))
                holder.actionIcon.contentDescription = context.getString(R.string.a11y_rule_notify_silent)
            } else {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_dont_notify))
                holder.actionIcon.contentDescription = context.getString(R.string.a11y_rule_notify_off)
            }

            val description = StringBuffer()
            pushRule.conditions?.forEachIndexed { i, condition ->
                if (i > 0) description.append("\n")
                description.append(condition.asExecutableCondition(pushRule)?.technicalDescription()
                        ?: "UNSUPPORTED")
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
