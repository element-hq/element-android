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
import butterknife.BindView
import butterknife.ButterKnife
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

        @BindView(R.id.troubleshootTestTitle)
        lateinit var titleText: TextView

        @BindView(R.id.troubleshootTestDescription)
        lateinit var descriptionText: TextView

        @BindView(R.id.troubleshootStatusIcon)
        lateinit var statusIconImage: ImageView

        @BindView(R.id.troubleshootProgressBar)
        lateinit var progressBar: ProgressBar

        @BindView(R.id.troubleshootTestButton)
        lateinit var fixButton: Button

        init {
            ButterKnife.bind(this, itemView)
        }

        fun bind(test: TroubleshootTest) {
            val context = itemView.context
            titleText.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_primary))
            descriptionText.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_secondary))

            when (test.status) {
                TroubleshootTest.TestStatus.NOT_STARTED      -> {
                    titleText.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_secondary))

                    progressBar.visibility = View.INVISIBLE
                    statusIconImage.visibility = View.VISIBLE
                    statusIconImage.setImageResource(R.drawable.unit_test)
                }
                TroubleshootTest.TestStatus.WAITING_FOR_USER -> {
                    progressBar.visibility = View.INVISIBLE
                    statusIconImage.visibility = View.VISIBLE
                    val infoColor = ContextCompat.getColor(context, R.color.vector_info_color)
                    val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_notification_privacy_warning)?.apply {
                        ThemeUtils.tintDrawableWithColor(this, infoColor)
                    }
                    statusIconImage.setImageDrawable(drawable)
                    descriptionText.setTextColor(infoColor)
                }
                TroubleshootTest.TestStatus.RUNNING          -> {
                    progressBar.visibility = View.VISIBLE
                    statusIconImage.visibility = View.INVISIBLE
                }
                TroubleshootTest.TestStatus.FAILED           -> {
                    progressBar.visibility = View.INVISIBLE
                    statusIconImage.visibility = View.VISIBLE
                    statusIconImage.setImageResource(R.drawable.unit_test_ko)

                    statusIconImage.imageTintList = null

                    descriptionText.setTextColor(ContextCompat.getColor(context, R.color.riotx_notice))
                }
                TroubleshootTest.TestStatus.SUCCESS          -> {
                    progressBar.visibility = View.INVISIBLE
                    statusIconImage.visibility = View.VISIBLE
                    statusIconImage.setImageResource(R.drawable.unit_test_ok)
                }
            }

            val quickFix = test.quickFix
            if (quickFix != null) {
                fixButton.setText(test.quickFix!!.title)
                fixButton.setOnClickListener { _ ->
                    test.quickFix!!.doFix()
                }
                fixButton.visibility = View.VISIBLE
            } else {
                fixButton.visibility = View.GONE
            }

            titleText.setText(test.titleResId)
            val description = test.description
            if (description == null) {
                descriptionText.visibility = View.GONE
            } else {
                descriptionText.visibility = View.VISIBLE
                descriptionText.text = description
            }
        }
    }
}
