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
package im.vector.app.features.home.room.list

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textview.MaterialTextView
import im.vector.app.R

class UnreadCounterBadgeView : MaterialTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun render(state: State) {
        if (state.count == 0) {
            visibility = View.INVISIBLE
        } else {
            visibility = View.VISIBLE
            val bgRes = if (state.highlighted) {
                R.drawable.bg_unread_highlight
            } else {
                R.drawable.bg_unread_notification
            }
            setBackgroundResource(bgRes)
            text = RoomSummaryFormatter.formatUnreadMessagesCounter(state.count)
        }
    }

    data class State(
            val count: Int,
            val highlighted: Boolean
    )
}
