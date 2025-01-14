/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.ViewSessionWarningInfoBinding
import im.vector.lib.strings.CommonStrings

class SessionWarningInfoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSessionWarningInfoBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.SessionWarningInfoView,
                0,
                0
        ).use {
            setDescription(it)
        }
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.SessionWarningInfoView_sessionsWarningInfoDescription)
        val hasLearnMore = typedArray.getBoolean(im.vector.lib.ui.styles.R.styleable.SessionWarningInfoView_sessionsWarningInfoHasLearnMore, false)
        if (hasLearnMore) {
            val learnMore = context.getString(CommonStrings.action_learn_more)
            val fullDescription = buildString {
                append(description)
                append(" ")
                append(learnMore)
            }

            binding.sessionWarningInfoDescription.setTextWithColoredPart(
                    fullText = fullDescription,
                    coloredPart = learnMore,
                    underline = false
            ) {
                onLearnMoreClickListener?.invoke()
            }
        } else {
            binding.sessionWarningInfoDescription.text = description
        }
    }
}
