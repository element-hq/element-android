/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.invite.AutoAcceptInvites

class FakeAutoAcceptInvites : AutoAcceptInvites {

    var _isEnabled: Boolean = false

    override val isEnabled: Boolean
        get() = _isEnabled
}
