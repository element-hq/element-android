/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database

import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver

internal interface LiveEntityObserver : SessionLifecycleObserver

internal abstract class RealmLiveEntityObserver<T : RealmObject>(protected val realmInstance: RealmInstance, coroutineDispatcher: CoroutineDispatcher) :
        LiveEntityObserver {

    protected val observerScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

    override fun onSessionStopped(session: Session) {
        observerScope.coroutineContext.cancelChildren()
    }

    override fun onClearCache(session: Session) {
        observerScope.coroutineContext.cancelChildren()
    }
}
