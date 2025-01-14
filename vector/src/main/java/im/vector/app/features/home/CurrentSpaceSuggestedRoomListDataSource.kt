/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.core.utils.BehaviorDataSource
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentSpaceSuggestedRoomListDataSource @Inject constructor() : BehaviorDataSource<List<SpaceChildInfo>>()
