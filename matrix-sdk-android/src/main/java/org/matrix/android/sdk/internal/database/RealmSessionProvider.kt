/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import android.os.Looper
import androidx.annotation.MainThread
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject
import kotlin.concurrent.getOrSet

/**
 * This class keeps an instance of realm open in the main thread so you can grab it whenever you want to get a realm
 * instance. This does check each time if you are on the main thread or not and returns the appropriate realm instance.
 */
@SessionScope
internal class RealmSessionProvider @Inject constructor(@SessionDatabase private val monarchy: Monarchy) :
        SessionLifecycleObserver {

    private val realmThreadLocal = ThreadLocal<Realm>()

    /**
     * Allow you to execute a block with an opened realm. It automatically closes it if necessary (ie. when not in main thread)
     */
    fun <R> withRealm(block: (Realm) -> R): R {
        return getRealmWrapper().withRealm(block)
    }

    @MainThread
    override fun onSessionStarted(session: Session) {
        realmThreadLocal.getOrSet {
            Realm.getInstance(monarchy.realmConfiguration)
        }
    }

    @MainThread
    override fun onSessionStopped(session: Session) {
        realmThreadLocal.get()?.close()
        realmThreadLocal.remove()
    }

    private fun getRealmWrapper(): RealmInstanceWrapper {
        val isOnMainThread = isOnMainThread()
        val realm = if (isOnMainThread) {
            realmThreadLocal.getOrSet {
                Realm.getInstance(monarchy.realmConfiguration)
            }
        } else {
            Realm.getInstance(monarchy.realmConfiguration)
        }
        return RealmInstanceWrapper(realm, closeRealmOnClose = !isOnMainThread)
    }

    private fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
