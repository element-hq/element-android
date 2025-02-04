/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.ActiveSessionDataSource
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toOptional

class FakeActiveSessionDataSource {

    val instance = ActiveSessionDataSource()

    fun setActiveSession(session: Session) {
        instance.post(session.toOptional())
    }
}
