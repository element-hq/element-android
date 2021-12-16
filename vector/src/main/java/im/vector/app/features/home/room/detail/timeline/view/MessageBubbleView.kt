/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.withStyledAttributes
import im.vector.app.R

class MessageBubbleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    var incoming: Boolean = false

    init {
        inflate(context, R.layout.view_message_bubble, this)
        context.withStyledAttributes(attrs, R.styleable.MessageBubble) {
            incoming = getBoolean(R.styleable.MessageBubble_incoming_style, false)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val currentLayoutDirection = layoutDirection
        if (incoming) {
            findViewById<View>(R.id.informationBottom).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.bubbleWrapper).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.bubbleView).layoutDirection = currentLayoutDirection
            findViewById<RelativeLayout>(R.id.bubbleView).setBackgroundResource(R.drawable.bg_timeline_incoming_message)
        } else {
            val oppositeLayoutDirection = if (currentLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
            findViewById<View>(R.id.informationBottom).layoutDirection = oppositeLayoutDirection
            findViewById<View>(R.id.bubbleWrapper).layoutDirection = oppositeLayoutDirection
            findViewById<View>(R.id.bubbleView).layoutDirection = currentLayoutDirection
            findViewById<RelativeLayout>(R.id.bubbleView).setBackgroundResource(R.drawable.bg_timeline_outgoing_message)
        }
    }
}
