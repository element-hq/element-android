/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.app.features.home.room.detail.timeline.item

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewPollResultLineBinding

class PollResultLineView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val views: ViewPollResultLineBinding

    var label: String? = null
        set(value) {
            field = value
            views.pollResultItemLabel.setTextOrHide(value)
        }

    var percent: String? = null
        set(value) {
            field = value
            views.pollResultItemPercent.setTextOrHide(value)
        }

    var optionSelected: Boolean = false
        set(value) {
            field = value
            views.pollResultItemSelectedIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var isWinner: Boolean = false
        set(value) {
            field = value
            // Text in main color
            views.pollResultItemLabel.setTypeface(views.pollResultItemLabel.typeface, if (value) Typeface.BOLD else Typeface.NORMAL)
            views.pollResultItemPercent.setTypeface(views.pollResultItemPercent.typeface, if (value) Typeface.BOLD else Typeface.NORMAL)
        }

    init {
        inflate(context, R.layout.view_poll_result_line, this)
        views = ViewPollResultLineBinding.bind(this)
        orientation = HORIZONTAL

        context.withStyledAttributes(attrs, R.styleable.PollResultLineView) {
            label = getString(R.styleable.PollResultLineView_optionName) ?: ""
            percent = getString(R.styleable.PollResultLineView_optionCount) ?: ""
            optionSelected = getBoolean(R.styleable.PollResultLineView_optionSelected, false)
            isWinner = getBoolean(R.styleable.PollResultLineView_optionIsWinner, false)
        }
    }
}
