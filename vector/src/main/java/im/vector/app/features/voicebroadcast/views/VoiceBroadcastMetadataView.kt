/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.voicebroadcast.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.use
import im.vector.app.R
import im.vector.app.databinding.ViewVoiceBroadcastMetadataBinding

class VoiceBroadcastMetadataView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val views = ViewVoiceBroadcastMetadataBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    var value: String
        get() = views.metadataText.text.toString()
        set(newValue) {
            views.metadataText.text = newValue
        }

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.VoiceBroadcastMetadataView,
                0,
                0
        ).use {
            setIcon(it)
            setValue(it)
        }
    }

    private fun setIcon(typedArray: TypedArray) {
        val icon = typedArray.getDrawable(im.vector.lib.ui.styles.R.styleable.VoiceBroadcastMetadataView_metadataIcon)
        views.metadataIcon.setImageDrawable(icon)
    }

    private fun setValue(typedArray: TypedArray) {
        val value = typedArray.getString(im.vector.lib.ui.styles.R.styleable.VoiceBroadcastMetadataView_metadataValue)
        views.metadataText.text = value
    }
}
