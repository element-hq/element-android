/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.onboarding.SelectedHomeserverState

object SelectedHomeserverStateFixture {

    fun aSelectedHomeserverState(
            userFacingUrl: String = "https://myhomeserver.com",
    ) = SelectedHomeserverState(userFacingUrl = userFacingUrl)
}
