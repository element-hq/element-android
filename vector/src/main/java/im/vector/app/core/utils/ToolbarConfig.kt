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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.appbar.MaterialToolbar

class ToolbarConfig(val activity: AppCompatActivity?, val toolbar: MaterialToolbar) {
    private var titleRes: Int? = null
    private var title: String? = null
    private var subtitleRes: Int? = null
    private var subtitle: String? = null
    private var customBackResId: Int? = null
    private var isBackAllowed: Boolean = true
    private var useCloseButton: Boolean = false

    fun withTitle(@StringRes titleRes: Int) = apply {this.titleRes = titleRes }
    fun withTitle(title: String) = apply {this.title = title}
    fun withSubtitle(@StringRes subtitleRes: Int) = apply {this.subtitleRes = subtitleRes }
    fun withSubtitle(subtitle: String) = apply {this.subtitle = subtitle}

    fun allowBack(isBackAllowed: Boolean) = apply { this.isBackAllowed = isBackAllowed }
    fun setBackAsClose(useCloseButton: Boolean) = apply { this.useCloseButton = useCloseButton }
    fun withCustomBackIcon(@DrawableRes customBackResId: Int) = apply { this.customBackResId = customBackResId }

    fun configure(){
        if (activity == null) {
            return
        }
        activity.setSupportActionBar(toolbar)
        toolbar.title = titleRes?.let { activity.getString(it) }
        toolbar.subtitle = subtitleRes?.let { activity.getString(it) }

        activity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(isBackAllowed)
                    it.setDisplayHomeAsUpEnabled(isBackAllowed)
        }

        customBackResId?.let {
            toolbar.navigationIcon = AppCompatResources.getDrawable(activity, it)
        }
    }
}
