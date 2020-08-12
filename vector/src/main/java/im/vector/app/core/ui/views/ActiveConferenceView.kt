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
import android.widget.TextView
import im.vector.app.R
import im.vector.app.core.utils.tappableMatchingText
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.widgets.model.Widget

class ActiveConferenceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onTapJoinAudio(jitsiWidget: Widget)
        fun onTapJoinVideo(jitsiWidget: Widget)
    }

    var callback: Callback? = null
    var jitsiWidget: Widget? = null

    init {
        setupView()
    }

    private fun setupView() {
        inflate(context, R.layout.view_active_conference_view, this)
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

        findViewById<TextView>(R.id.activeConferenceInfo).apply {
            text = styledText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
