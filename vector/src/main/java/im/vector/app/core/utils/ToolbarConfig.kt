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
class ToolbarConfig(val activity: AppCompatActivity?, val toolbar: MaterialToolbar) {
    private var customBackResId: Int? = null

    fun setup() {
        if (activity == null) {
            return
        }
        activity.setSupportActionBar(toolbar)
    }

    /**
     * Delegating property for [toolbar.title]
     * */
    var title: CharSequence? by toolbar::title

    /**
     * Delegating property for [toolbar.subtitle]
     * */
    var subtitle: CharSequence? by toolbar::subtitle

    /**
     * Sets toolbar's title text
     * */
    fun setTitle(title: CharSequence?) = apply { toolbar.title = title }

    /**
     * Sets toolbar's title text using provided string resource
     * */
    fun setTitle(@StringRes titleRes: Int) = apply { toolbar.setTitle(titleRes)}

    /**
     * Sets toolbar's subtitle text
     * */
    fun setSubtitle(subtitle: String?) = apply { toolbar.subtitle = subtitle }

    /**
     * Sets toolbar's title text using provided string resource
     * */
    fun setSubtitle(@StringRes subtitleRes: Int) = apply { toolbar.subtitle = activity?.getString(subtitleRes) }

    /**
     * Enables/disables navigate back button
     *
     * @param isAllowed defines if back button is enabled. Default [true]
     * @param useCross defines if cross icon should be used instead of arrow. Default [false]
     * */
    fun allowBack(isAllowed: Boolean = true, useCross: Boolean = false) = apply {
        activity?.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(isAllowed)
            it.setDisplayHomeAsUpEnabled(isAllowed)
            if (isAllowed && useCross) {
                val navResId = customBackResId ?: R.drawable.ic_x_18dp
                toolbar.navigationIcon = AppCompatResources.getDrawable(activity, navResId)
            }
        }
    }
}
