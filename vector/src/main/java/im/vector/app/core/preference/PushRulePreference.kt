/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import im.vector.app.features.settings.notifications.NotificationIndex
import im.vector.lib.strings.CommonStrings

class PushRulePreference : VectorPreference {

    /**
     * @return the selected push rule index
     */
    var index: NotificationIndex? = null
        private set

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        layoutResource = R.layout.vector_preference_push_rule
    }

    /**
     * Update the notification index.
     *
     * @param notificationIndex the new notification index
     */
    fun setIndex(notificationIndex: NotificationIndex?) {
        index = notificationIndex
        refreshSummary()
    }

    /**
     * Refresh the summary.
     */
    private fun refreshSummary() {
        summary = context.getString(
                when (index) {
                    NotificationIndex.OFF -> CommonStrings.notification_off
                    NotificationIndex.SILENT -> CommonStrings.notification_silent
                    NotificationIndex.NOISY, null -> CommonStrings.notification_noisy
                }
        )
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(android.R.id.summary)?.visibility = View.GONE
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        val radioGroup = holder.findViewById(R.id.bingPreferenceRadioGroup) as? RadioGroup
        radioGroup?.setOnCheckedChangeListener(null)

        when (index) {
            NotificationIndex.OFF -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleOff)
            }
            NotificationIndex.SILENT -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleSilent)
            }
            NotificationIndex.NOISY -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleNoisy)
            }
            null -> Unit
        }

        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.bingPreferenceRadioBingRuleOff -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.OFF)
                }
                R.id.bingPreferenceRadioBingRuleSilent -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.SILENT)
                }
                R.id.bingPreferenceRadioBingRuleNoisy -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NotificationIndex.NOISY)
                }
            }
        }
    }
}
