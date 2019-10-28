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

package im.vector.riotx.features.html

import android.content.Context
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import im.vector.riotx.R
import im.vector.riotx.features.themes.ThemeUtils

class SpoilerSpan(val bgColor: Int, val context: Context) : ClickableSpan() {

    override fun onClick(widget: View) {
        isHidden = !isHidden
        widget.invalidate()
    }

    var isHidden = true

    override fun updateDrawState(tp: TextPaint) {
        tp.bgColor = bgColor
        if (isHidden) {
            tp.color = Color.TRANSPARENT
        } else {
            tp.color = ThemeUtils.getColor(context, R.attr.riotx_text_primary)
        }
    }
}
