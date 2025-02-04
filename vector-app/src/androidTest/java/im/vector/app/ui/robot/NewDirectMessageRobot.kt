/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import im.vector.app.R
import im.vector.app.waitForView
import im.vector.lib.strings.CommonStrings

class NewDirectMessageRobot {

    fun verifyQrCodeButton() {
        Espresso.onView(ViewMatchers.withId(R.id.userListRecyclerView))
                .perform(waitForView(ViewMatchers.withText(CommonStrings.qr_code)))
    }

    fun verifyInviteFriendsButton() {
        Espresso.onView(ViewMatchers.withId(R.id.userListRecyclerView))
                .perform(waitForView(ViewMatchers.withText(CommonStrings.invite_friends)))
    }
}
