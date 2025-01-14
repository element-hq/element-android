/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`

fun clickOnPreference(@StringRes textResId: Int) {
    onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                    actionOnItem<RecyclerView.ViewHolder>(
                            allOf(
                                    hasDescendant(withText(textResId)),
                                    // Avoid to click on the Preference Category
                                    isFocusable()
                            ), click()
                    )
            )
}

fun clickOnSwitchPreference(preferenceKey: String) {
    onData(allOf(`is`(instanceOf(Preference::class.java)), withKey(preferenceKey)))
            .onChildView(withClassName(`is`(Switch::class.java.name))).perform(click())
}
