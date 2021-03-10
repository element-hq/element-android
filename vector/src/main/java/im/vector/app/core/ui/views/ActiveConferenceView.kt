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

package im.vector.app.core.ui.views

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.tappableMatchingText
import im.vector.app.databinding.ViewActiveConferenceViewBinding
import im.vector.app.features.home.room.detail.RoomDetailViewState
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType

class ActiveConferenceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapJoinAudio(jitsiWidget: Widget)
        fun onTapJoinVideo(jitsiWidget: Widget)
        fun onDelete(jitsiWidget: Widget)
    }

    var callback: Callback? = null
    private var jitsiWidget: Widget? = null

    private lateinit var views: ViewActiveConferenceViewBinding

    init {
        setupView()
    }

    private fun setupView() {
        inflate(context, R.layout.view_active_conference_view, this)
        views = ViewActiveConferenceViewBinding.bind(this)
        setBackgroundColor(ThemeUtils.getColor(context, R.attr.colorPrimary))

        // "voice" and "video" texts are underlined and clickable
        val voiceString = context.getString(R.string.ongoing_conference_call_voice)
        val videoString = context.getString(R.string.ongoing_conference_call_video)

        val fullMessage = context.getString(R.string.ongoing_conference_call, voiceString, videoString)

        val styledText = SpannableString(fullMessage)
        styledText.tappableMatchingText(voiceString, object : ClickableSpan() {
            override fun onClick(widget: View) {
                jitsiWidget?.let {
                    callback?.onTapJoinAudio(it)
                }
            }
        })
        styledText.tappableMatchingText(videoString, object : ClickableSpan() {
            override fun onClick(widget: View) {
                jitsiWidget?.let {
                    callback?.onTapJoinVideo(it)
                }
            }
        })

        views.activeConferenceInfo.apply {
            text = styledText
            movementMethod = LinkMovementMethod.getInstance()
        }

        views.deleteWidgetButton.setOnClickListener {
            jitsiWidget?.let { callback?.onDelete(it) }
        }
    }

    fun render(state: RoomDetailViewState) {
        val summary = state.asyncRoomSummary()
        if (summary?.membership == Membership.JOIN) {
            // We only display banner for 'live' widgets
            jitsiWidget = state.activeRoomWidgets()?.firstOrNull {
                // for now only jitsi?
                it.type == WidgetType.Jitsi
            }

            isVisible = jitsiWidget != null
            // if sent by me or if i can moderate?
            views.deleteWidgetButton.isVisible = state.isAllowedToManageWidgets
        } else {
            isVisible = false
        }
    }
}
