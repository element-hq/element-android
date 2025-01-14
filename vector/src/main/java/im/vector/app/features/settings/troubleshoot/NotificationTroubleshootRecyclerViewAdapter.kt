/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.databinding.ItemNotificationTroubleshootBinding
import im.vector.app.features.themes.ThemeUtils

class NotificationTroubleshootRecyclerViewAdapter(val tests: ArrayList<TroubleshootTest>) :
        RecyclerView.Adapter<NotificationTroubleshootRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(viewType, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_notification_troubleshoot

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val test = tests[position]
        holder.bind(test)
    }

    override fun getItemCount(): Int = tests.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val views = ItemNotificationTroubleshootBinding.bind(itemView)

        fun bind(test: TroubleshootTest) {
            val context = itemView.context
            views.troubleshootTestTitle.setTextColor(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            views.troubleshootTestDescription.setTextColor(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary))

            when (test.status) {
                TroubleshootTest.TestStatus.NOT_STARTED -> {
                    views.troubleshootTestTitle.setTextColor(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary))

                    views.troubleshootProgressBar.visibility = View.INVISIBLE
                    views.troubleshootStatusIcon.visibility = View.VISIBLE
                    views.troubleshootStatusIcon.setImageResource(R.drawable.unit_test)
                }
                TroubleshootTest.TestStatus.WAITING_FOR_USER -> {
                    views.troubleshootProgressBar.visibility = View.INVISIBLE
                    views.troubleshootStatusIcon.visibility = View.VISIBLE
                    val infoColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.vector_info_color)
                    val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_notification_privacy_warning)?.apply {
                        ThemeUtils.tintDrawableWithColor(this, infoColor)
                    }
                    views.troubleshootStatusIcon.setImageDrawable(drawable)
                    views.troubleshootTestDescription.setTextColor(infoColor)
                }
                TroubleshootTest.TestStatus.RUNNING -> {
                    views.troubleshootProgressBar.visibility = View.VISIBLE
                    views.troubleshootStatusIcon.visibility = View.INVISIBLE
                }
                TroubleshootTest.TestStatus.FAILED -> {
                    views.troubleshootProgressBar.visibility = View.INVISIBLE
                    views.troubleshootStatusIcon.visibility = View.VISIBLE
                    views.troubleshootStatusIcon.setImageResource(R.drawable.unit_test_ko)
                    views.troubleshootStatusIcon.imageTintList = null
                    views.troubleshootTestDescription.setTextColor(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError))
                }
                TroubleshootTest.TestStatus.SUCCESS -> {
                    views.troubleshootProgressBar.visibility = View.INVISIBLE
                    views.troubleshootStatusIcon.visibility = View.VISIBLE
                    views.troubleshootStatusIcon.setImageResource(R.drawable.unit_test_ok)
                }
            }

            val quickFix = test.quickFix
            if (quickFix != null) {
                views.troubleshootTestButton.setText(test.quickFix!!.title)
                views.troubleshootTestButton.setOnClickListener {
                    test.quickFix!!.doFix()
                }
                views.troubleshootTestButton.visibility = View.VISIBLE
            } else {
                views.troubleshootTestButton.visibility = View.GONE
            }

            views.troubleshootTestTitle.setText(test.titleResId)
            val description = test.description
            if (description == null) {
                views.troubleshootTestDescription.visibility = View.GONE
            } else {
                views.troubleshootTestDescription.visibility = View.VISIBLE
                views.troubleshootTestDescription.text = description
            }
        }
    }
}
