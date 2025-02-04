/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.session

import org.matrix.android.sdk.api.session.Session
import kotlin.reflect.KProperty

/**
 * This is a simple hack for having some Session scope dependencies.
 * Probably a temporary solution waiting for refactoring the Dagger management of Session.
 * You should use it with an extension property :
val Session.myProperty: MyProperty by SessionScopedProperty {
init code
}
 *
 */
class SessionScopedProperty<T : Any>(val initializer: (Session) -> T) {

    private val propertyBySessionId = HashMap<String, T>()

    private val sessionListener = object : Session.Listener {

        override fun onSessionStopped(session: Session) {
            synchronized(propertyBySessionId) {
                session.removeListener(this)
                propertyBySessionId.remove(session.sessionId)
            }
        }
    }

    operator fun getValue(thisRef: Session, property: KProperty<*>): T = synchronized(propertyBySessionId) {
        propertyBySessionId.getOrPut(thisRef.sessionId) {
            thisRef.addListener(sessionListener)
            initializer(thisRef)
        }
    }
}
