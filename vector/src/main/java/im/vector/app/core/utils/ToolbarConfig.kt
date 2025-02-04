/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.appbar.MaterialToolbar
import im.vector.app.R

/**
 * Helper class to configure toolbar.
 * Wraps [MaterialToolbar] providing set of methods to configure it
 */
class ToolbarConfig(val activity: AppCompatActivity, val toolbar: MaterialToolbar) {
    private var customBackResId: Int? = null

    fun setup() = apply {
        activity.setSupportActionBar(toolbar)
    }

    /**
     * Delegating property for [activity.supportActionBar?.title].
     */
    var title: CharSequence?
        set(value) {
            setTitle(value)
        }
        get() = activity.supportActionBar?.title

    /**
     * Delegating property for [activity.supportActionBar?.subtitle].
     */
    var subtitle: CharSequence?
        set(value) {
            setSubtitle(value)
        }
        get() = activity.supportActionBar?.subtitle

    /**
     * Sets toolbar's title text.
     */
    fun setTitle(title: CharSequence?) = apply { activity.supportActionBar?.title = title }

    /**
     * Sets toolbar's title text using provided string resource.
     */
    fun setTitle(@StringRes titleRes: Int) = apply { activity.supportActionBar?.setTitle(titleRes) }

    /**
     * Sets toolbar's subtitle text.
     */
    fun setSubtitle(subtitle: CharSequence?) = apply { activity.supportActionBar?.subtitle = subtitle }

    /**
     * Sets toolbar's title text using provided string resource.
     */
    fun setSubtitle(@StringRes subtitleRes: Int) = apply { activity.supportActionBar?.setSubtitle(subtitleRes) }

    /**
     * Enables/disables navigate back button.
     *
     * @param isAllowed defines if back button is enabled. Default [true]
     * @param useCross defines if cross icon should be used instead of arrow. Default [false]
     */
    fun allowBack(isAllowed: Boolean = true, useCross: Boolean = false) = apply {
        activity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(isAllowed)
            it.setDisplayHomeAsUpEnabled(isAllowed)
            if (isAllowed && useCross) {
                val navResId = customBackResId ?: R.drawable.ic_x_18dp
                toolbar.navigationIcon = AppCompatResources.getDrawable(activity, navResId)
            }
        }
    }
}
