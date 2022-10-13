/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.debug

import io.realm.kotlin.RealmConfiguration
import org.matrix.android.sdk.api.debug.DebugService
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.tools.RealmDebugTools
import org.matrix.android.sdk.internal.di.AuthDatabase
import org.matrix.android.sdk.internal.di.GlobalDatabase
import javax.inject.Inject

internal class DefaultDebugService @Inject constructor(
        @AuthDatabase private val authRealmInstance: RealmInstance,
        @GlobalDatabase private val globalRealmInstance: RealmInstance,
        private val sessionManager: SessionManager,
) : DebugService {

    override fun getAllRealmConfigurations(): List<RealmConfiguration> {
        return sessionManager.getLastSession()?.getRealmConfigurations().orEmpty() +
                authRealmInstance.realmConfiguration + globalRealmInstance.realmConfiguration
    }

    override fun getDbUsageInfo() = buildString {
        append(RealmDebugTools(authRealmInstance).getInfo("Auth"))
        append(RealmDebugTools(globalRealmInstance).getInfo("Global"))
        append(sessionManager.getLastSession()?.getDbUsageInfo())
    }
}
