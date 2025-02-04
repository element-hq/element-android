/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.core.extensions.setAttributeBackground
import im.vector.app.databinding.ViewSessionOverviewEntryBinding

class SessionOverviewEntryView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSessionOverviewEntryBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        initBackground()
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.SessionOverviewEntryView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
        }
    }

    private fun initBackground() {
        binding.root.setAttributeBackground(android.R.attr.selectableItemBackground)
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(im.vector.lib.ui.styles.R.styleable.SessionOverviewEntryView_sessionOverviewEntryTitle)
        binding.sessionsOverviewEntryTitle.text = title
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.SessionOverviewEntryView_sessionOverviewEntryDescription)
        binding.sessionsOverviewEntryDescription.text = description
    }
}
