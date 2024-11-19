/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app

import im.vector.app.core.utils.BehaviorDataSource
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionDataSource @Inject constructor() : BehaviorDataSource<Optional<Session>>()
