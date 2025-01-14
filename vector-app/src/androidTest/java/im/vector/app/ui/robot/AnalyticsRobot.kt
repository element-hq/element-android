/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.analytics.ui.consent.AnalyticsOptInActivity
import im.vector.lib.strings.CommonStrings

class AnalyticsRobot {

    fun optIn() {
        answerOptIn(true)
    }

    fun optOut() {
        answerOptIn(false)
    }

    private fun answerOptIn(optIn: Boolean) {
        waitUntilActivityVisible<AnalyticsOptInActivity> {
            waitUntilViewVisible(withId(R.id.title))
        }
        assertDisplayed(R.id.title, CommonStrings.analytics_opt_in_title)
        if (optIn) {
            clickOn(R.id.submit)
        } else {
            clickOn(R.id.later)
        }
    }
}
