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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import im.vector.app.R

class RevealPasswordImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        render(false)
    }

    fun render(isPasswordShown: Boolean) {
        if (isPasswordShown) {
            contentDescription = context.getString(R.string.a11y_hide_password)
            setImageResource(R.drawable.ic_eye_closed)
        } else {
            contentDescription = context.getString(R.string.a11y_show_password)
            setImageResource(R.drawable.ic_eye)
        }
    }
}
