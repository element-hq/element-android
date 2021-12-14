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
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isInvisible
import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.item.SendStateDecoration
import im.vector.app.features.themes.ThemeUtils

class SendStateImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        if (isInEditMode) {
            render(SendStateDecoration.SENT)
        }
    }

    fun render(sendState: SendStateDecoration) {
        isInvisible = when (sendState) {
            SendStateDecoration.SENDING_NON_MEDIA -> {
                setImageResource(R.drawable.ic_sending_message)
                imageTintList = ColorStateList.valueOf(ThemeUtils.getColor(context, R.attr.vctr_content_tertiary))
                contentDescription = context.getString(R.string.event_status_a11y_sending)
                false
            }
            SendStateDecoration.SENT              -> {
                setImageResource(R.drawable.ic_message_sent)
                imageTintList = ColorStateList.valueOf(ThemeUtils.getColor(context, R.attr.vctr_content_tertiary))
                contentDescription = context.getString(R.string.event_status_a11y_sent)
                false
            }
            SendStateDecoration.FAILED            -> {
                setImageResource(R.drawable.ic_sending_message_failed)
                imageTintList = null
                contentDescription = context.getString(R.string.event_status_a11y_failed)
                false
            }
            SendStateDecoration.SENDING_MEDIA,
            SendStateDecoration.NONE              -> {
                true
            }
        }
    }
}
