/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmConfiguration
import io.realm.RealmObject
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal interface LiveEntityObserver : SessionLifecycleObserver

internal abstract class RealmLiveEntityObserver<T : RealmObject>(protected val realmConfiguration: RealmConfiguration) :
        LiveEntityObserver, RealmChangeListener<RealmResults<T>> {

    private companion object {
        val BACKGROUND_HANDLER = createBackgroundHandler("Matrix-LIVE_ENTITY_BACKGROUND")
    }

    protected val observerScope = CoroutineScope(SupervisorJob() + BACKGROUND_HANDLER.asCoroutineDispatcher())
    protected abstract val query: Monarchy.Query<T>
    private val isStarted = AtomicBoolean(false)
    private val backgroundRealm = AtomicReference<Realm>()
    private lateinit var results: AtomicReference<RealmResults<T>>

    override fun onSessionStarted(session: Session) {
        if (isStarted.compareAndSet(false, true)) {
            BACKGROUND_HANDLER.post {
                val realm = Realm.getInstance(realmConfiguration)
                backgroundRealm.set(realm)
                val queryResults = query.createQuery(realm).findAll()
                queryResults.addChangeListener(this)
                results = AtomicReference(queryResults)
            }
        }
    }

    override fun onSessionStopped(session: Session) {
        if (isStarted.compareAndSet(true, false)) {
            BACKGROUND_HANDLER.post {
                results.getAndSet(null).removeAllChangeListeners()
                backgroundRealm.getAndSet(null).also {
                    it.close()
                }
                observerScope.coroutineContext.cancelChildren()
            }
        }
    }

    override fun onClearCache(session: Session) {
        observerScope.coroutineContext.cancelChildren()
    }
}
