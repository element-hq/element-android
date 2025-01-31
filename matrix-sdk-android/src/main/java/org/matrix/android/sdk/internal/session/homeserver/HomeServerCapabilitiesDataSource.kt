/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.homeserver

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.internal.database.mapper.HomeServerCapabilitiesMapper
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class HomeServerCapabilitiesDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy
) {
    fun getHomeServerCapabilities(): HomeServerCapabilities? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.let {
                HomeServerCapabilitiesMapper.map(it)
            }
        }
    }
}
