/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.espresso.tools

import android.widget.Switch
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.PreferenceMatchers.withKey
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import im.vector.app.R
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`

fun clickOnPreference(@StringRes textResId: Int) {
    onView(withId(R.id.recycler_view))
            .perform(actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(textResId)), click()))
}

fun clickOnSwitchPreference(preferenceKey: String) {
    onData(allOf(`is`(instanceOf(Preference::class.java)), withKey(preferenceKey)))
            .onChildView(withClassName(`is`(Switch::class.java.name))).perform(click())
}
