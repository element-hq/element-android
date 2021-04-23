/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.app.features.settings.troubleshoot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.features.themes.ThemeUtils

class NotificationTroubleshootRecyclerViewAdapter(val tests: ArrayList<TroubleshootTest>)
    : RecyclerView.Adapter<NotificationTroubleshootRecyclerViewAdapter.ViewHolder>() {

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
        private val troubleshootProgressBar = itemView.findViewById<ProgressBar>(R.id.troubleshootProgressBar)
        private val troubleshootTestTitle = itemView.findViewById<TextView>(R.id.troubleshootTestTitle)
        private val troubleshootTestDescription = itemView.findViewById<TextView>(R.id.troubleshootTestDescription)
        private val troubleshootStatusIcon = itemView.findViewById<ImageView>(R.id.troubleshootStatusIcon)
        private val troubleshootTestButton = itemView.findViewById<Button>(R.id.troubleshootTestButton)

        fun bind(test: TroubleshootTest) {
            val context = itemView.context
            troubleshootTestTitle.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_primary))
            troubleshootTestDescription.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_secondary))

            when (test.status) {
                TroubleshootTest.TestStatus.NOT_STARTED      -> {
                    troubleshootTestTitle.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_secondary))

                    troubleshootProgressBar.visibility = View.INVISIBLE
                    troubleshootStatusIcon.visibility = View.VISIBLE
                    troubleshootStatusIcon.setImageResource(R.drawable.unit_test)
                }
                TroubleshootTest.TestStatus.WAITING_FOR_USER -> {
                    troubleshootProgressBar.visibility = View.INVISIBLE
                    troubleshootStatusIcon.visibility = View.VISIBLE
                    val infoColor = ContextCompat.getColor(context, R.color.vector_info_color)
                    val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_notification_privacy_warning)?.apply {
                        ThemeUtils.tintDrawableWithColor(this, infoColor)
                    }
                    troubleshootStatusIcon.setImageDrawable(drawable)
                    troubleshootTestDescription.setTextColor(infoColor)
                }
                TroubleshootTest.TestStatus.RUNNING          -> {
                    troubleshootProgressBar.visibility = View.VISIBLE
                    troubleshootStatusIcon.visibility = View.INVISIBLE
                }
                TroubleshootTest.TestStatus.FAILED           -> {
                    troubleshootProgressBar.visibility = View.INVISIBLE
                    troubleshootStatusIcon.visibility = View.VISIBLE
                    troubleshootStatusIcon.setImageResource(R.drawable.unit_test_ko)

                    troubleshootStatusIcon.imageTintList = null

                    troubleshootTestDescription.setTextColor(ContextCompat.getColor(context, R.color.riotx_notice))
                }
                TroubleshootTest.TestStatus.SUCCESS          -> {
                    troubleshootProgressBar.visibility = View.INVISIBLE
                    troubleshootStatusIcon.visibility = View.VISIBLE
                    troubleshootStatusIcon.setImageResource(R.drawable.unit_test_ok)
                }
            }

            val quickFix = test.quickFix
            if (quickFix != null) {
                troubleshootTestButton.setText(test.quickFix!!.title)
                troubleshootTestButton.setOnClickListener {
                    test.quickFix!!.doFix()
                }
                troubleshootTestButton.visibility = View.VISIBLE
            } else {
                troubleshootTestButton.visibility = View.GONE
            }

            troubleshootTestTitle.setText(test.titleResId)
            val description = test.description
            if (description == null) {
                troubleshootTestDescription.visibility = View.GONE
            } else {
                troubleshootTestDescription.visibility = View.VISIBLE
                troubleshootTestDescription.text = description
            }
        }
    }
}
