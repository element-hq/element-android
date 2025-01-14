/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.ViewSessionsListHeaderBinding
import im.vector.lib.strings.CommonStrings

class SessionsListHeaderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSessionsListHeaderBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    val menu: Menu = binding.sessionsListHeaderMenu.menu
    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.SessionsListHeaderView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
            setMenu(it)
        }
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(im.vector.lib.ui.styles.R.styleable.SessionsListHeaderView_sessionsListHeaderTitle)
        binding.sessionsListHeaderTitle.setTextOrHide(title)
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.SessionsListHeaderView_sessionsListHeaderDescription)
        if (description.isNullOrEmpty()) {
            binding.sessionsListHeaderDescription.isVisible = false
            return
        }

        val hasLearnMoreLink = typedArray.getBoolean(im.vector.lib.ui.styles.R.styleable.SessionsListHeaderView_sessionsListHeaderHasLearnMoreLink, true)
        if (hasLearnMoreLink) {
            setDescriptionWithLearnMore(description)
        } else {
            binding.sessionsListHeaderDescription.text = description
        }

        binding.sessionsListHeaderDescription.isVisible = true
    }

    private fun setDescriptionWithLearnMore(description: String) {
        val learnMore = context.getString(CommonStrings.action_learn_more)
        val fullDescription = buildString {
            append(description)
            append(" ")
            append(learnMore)
        }
        binding.sessionsListHeaderDescription.setTextWithColoredPart(
                fullText = fullDescription,
                coloredPart = learnMore,
                underline = false
        ) {
            onLearnMoreClickListener?.invoke()
        }
    }

    @Suppress("RestrictedApi")
    private fun setMenu(typedArray: TypedArray) {
        val menuResId = typedArray.getResourceId(im.vector.lib.ui.styles.R.styleable.SessionsListHeaderView_sessionsListHeaderMenu, -1)
        if (menuResId == -1) {
            binding.sessionsListHeaderMenu.isVisible = false
        } else {
            binding.sessionsListHeaderMenu.showOverflowMenu()
            val menuBuilder = binding.sessionsListHeaderMenu.menu as? MenuBuilder
            menuBuilder?.let { MenuInflater(context).inflate(menuResId, it) }
        }
    }

    fun setOnMenuItemClickListener(listener: OnMenuItemClickListener) {
        binding.sessionsListHeaderMenu.setOnMenuItemClickListener(listener)
    }
}
