/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.ViewSessionsListHeaderBinding

class SessionsListHeaderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSessionsListHeaderBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        context.obtainStyledAttributes(
                attrs,
                R.styleable.SessionsListHeaderView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
        }
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(R.styleable.SessionsListHeaderView_sessionsListHeaderTitle)
        binding.sessionsListHeaderTitle.setTextOrHide(title)
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(R.styleable.SessionsListHeaderView_sessionsListHeaderDescription)
        if (description.isNullOrEmpty()) {
            binding.sessionsListHeaderDescription.isVisible = false
            return
        }

        val learnMore = context.getString(R.string.action_learn_more)
        val fullDescription = buildString {
            append(description)
            append(" ")
            append(learnMore)
        }

        binding.sessionsListHeaderDescription.isVisible = true
        binding.sessionsListHeaderDescription.setTextWithColoredPart(
                fullText = fullDescription,
                coloredPart = learnMore,
                underline = false
        ) {
            onLearnMoreClickListener?.invoke()
        }
    }
}
