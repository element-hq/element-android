/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
