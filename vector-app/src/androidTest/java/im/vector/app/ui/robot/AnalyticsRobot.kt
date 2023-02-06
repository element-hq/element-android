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

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.analytics.ui.consent.AnalyticsOptInActivity

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
        assertDisplayed(R.id.title, R.string.analytics_opt_in_title)
        if (optIn) {
            clickOn(R.id.submit)
        } else {
            clickOn(R.id.later)
        }
    }
}
