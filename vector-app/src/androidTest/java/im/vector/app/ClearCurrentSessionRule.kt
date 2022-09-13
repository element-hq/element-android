/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.EntryPoints
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.features.analytics.store.AnalyticsStore
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.reflect.KClass

/**
 * A TestRule to reset and clear the current Session.
 * If a Session is active it will be signed out and cleared from the ActiveSessionHolder.
 * The VectorPreferences and AnalyticsDatastore are also cleared in an attempt to recreate a fresh base.
 */
class ClearCurrentSessionRule : TestWatcher() {
    override fun apply(base: Statement, description: Description): Statement {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            reflectAnalyticDatastore(context).edit { it.clear() }
            runCatching {
                val entryPoint = EntryPoints.get(context.applicationContext, SingletonEntryPoint::class.java)
                val sessionHolder = entryPoint.activeSessionHolder()
                sessionHolder.getSafeActiveSession()?.signOutService()?.signOut(true)
                entryPoint.vectorPreferences().clearPreferences()
                sessionHolder.clearActiveSession()
            }
        }
        return super.apply(base, description)
    }
}

private fun KClass<*>.asTopLevel() = Class.forName("${qualifiedName}Kt")

/**
 * Fetches the top level, private [Context.dataStore] extension property from [im.vector.app.features.analytics.store.AnalyticsStore]
 * via reflection to avoid exposing property to all callers.
 */
@Suppress("UNCHECKED_CAST")
private fun reflectAnalyticDatastore(context: Context): DataStore<Preferences> {
    val klass = AnalyticsStore::class.asTopLevel()
    val method = klass.getMethod("access\$getDataStore", Context::class.java)
    return method.invoke(klass, context) as DataStore<Preferences>
}
