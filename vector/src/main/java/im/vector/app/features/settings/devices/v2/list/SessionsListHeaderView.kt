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

    val menu: Menu = binding.sessionsListHeaderMenu.menu
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
            setMenu(it)
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

        val hasLearnMoreLink = typedArray.getBoolean(R.styleable.SessionsListHeaderView_sessionsListHeaderHasLearnMoreLink, true)
        if (hasLearnMoreLink) {
            setDescriptionWithLearnMore(description)
        } else {
            binding.sessionsListHeaderDescription.text = description
        }

        binding.sessionsListHeaderDescription.isVisible = true
    }

    private fun setDescriptionWithLearnMore(description: String) {
        val learnMore = context.getString(R.string.action_learn_more)
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
        val menuResId = typedArray.getResourceId(R.styleable.SessionsListHeaderView_sessionsListHeaderMenu, -1)
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
