package im.vector.riotredesign.features.settings.push

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
import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.features.notifications.NotificationAction


@EpoxyModelClass(layout = R.layout.item_pushrule_raw)
abstract class PushRuleItem : EpoxyModelWithHolder<PushRuleItem.Holder>() {

    @EpoxyAttribute
    lateinit var pushRule: PushRule

    // TODO i18n
    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        val context = holder.view.context
        if (pushRule.enabled) {
            holder.view.setBackgroundColor(Color.TRANSPARENT)
            holder.ruleId.text = pushRule.ruleId
        } else {
            holder.view.setBackgroundColor(ContextCompat.getColor(context, R.color.vector_silver_color))
            holder.ruleId.text = "[Disabled] ${pushRule.ruleId}"
        }
        val actions = Action.mapFrom(pushRule)
        if (actions.isNullOrEmpty()) {
            holder.actionIcon.isInvisible = true
        } else {
            holder.actionIcon.isVisible = true
            val notifAction = NotificationAction.extractFrom(actions)

            if (notifAction.shouldNotify && !notifAction.soundName.isNullOrBlank()) {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_notify_noisy))
            } else if (notifAction.shouldNotify) {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_notify_silent))
            } else {
                holder.actionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_dont_notify))
            }

            val description = StringBuffer()
            pushRule.conditions?.forEachIndexed { i, condition ->
                if (i > 0) description.append("\n")
                description.append(condition.asExecutableCondition()?.technicalDescription()
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