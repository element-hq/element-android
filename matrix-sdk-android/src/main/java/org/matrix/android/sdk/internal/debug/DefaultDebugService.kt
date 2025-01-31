/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.debug

import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.debug.DebugService
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.database.tools.RealmDebugTools
import org.matrix.android.sdk.internal.di.AuthDatabase
import org.matrix.android.sdk.internal.di.GlobalDatabase
import javax.inject.Inject

internal class DefaultDebugService @Inject constructor(
        @AuthDatabase private val realmConfigurationAuth: RealmConfiguration,
        @GlobalDatabase private val realmConfigurationGlobal: RealmConfiguration,
        private val sessionManager: SessionManager,
) : DebugService {

    override fun getAllRealmConfigurations(): List<RealmConfiguration> {
        return sessionManager.getLastSession()?.getRealmConfigurations().orEmpty() +
                realmConfigurationAuth +
                realmConfigurationGlobal
    }

    override fun getDbUsageInfo() = buildString {
        append(RealmDebugTools(realmConfigurationAuth).getInfo("Auth"))
        append(RealmDebugTools(realmConfigurationGlobal).getInfo("Global"))
        append(sessionManager.getLastSession()?.getDbUsageInfo())
    }
}
