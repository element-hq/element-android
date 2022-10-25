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

package im.vector.app.ui.robot

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R

class LayoutPreferencesRobot {

    fun crawl() {
        toggleRecents()
        toggleFilters()
        useAZOrderd()
        useActivityOrder()
    }

    fun toggleRecents() {
        clickOn(R.id.home_layout_settings_recents)
    }

    fun toggleFilters() {
        clickOn(R.id.home_layout_settings_filters)
    }

    fun useAZOrderd() {
        clickOn(R.id.home_layout_settings_sort_name)
    }

    fun useActivityOrder() {
        clickOn(R.id.home_layout_settings_sort_activity)
    }
}
