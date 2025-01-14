/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import im.vector.app.features.call.lookup.CallProtocolsChecker
import im.vector.app.features.call.lookup.CallUserMapper
import im.vector.app.features.session.SessionScopedProperty
import org.matrix.android.sdk.api.session.Session

interface VectorCallService {
    val protocolChecker: CallProtocolsChecker
    val userMapper: CallUserMapper
}

val Session.vectorCallService: VectorCallService by SessionScopedProperty {
    object : VectorCallService {
        override val protocolChecker = CallProtocolsChecker(it)
        override val userMapper = CallUserMapper(it, protocolChecker)
    }
}
