/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Provides a singleton datastore cache.
 * Allows for lazily fetching a datastore instance by key to avoid creating multiple stores for the same file.
 * Based on https://androidx.tech/artifacts/datastore/datastore-preferences/1.0.0-source/androidx/datastore/preferences/PreferenceDataStoreDelegate.kt.html.
 *
 * Makes use of a ReadOnlyProperty in order to provide a simplified api on top of a Context.
 * ReadOnlyProperty allows us to lazily access the backing property instead of requiring it upfront as a dependency.
 * <pre>
 * val Context.dataStoreProvider by dataStoreProvider()
 * </pre>
 */
fun dataStoreProvider(): ReadOnlyProperty<Context, (String) -> DataStore<Preferences>> {
    return MappedPreferenceDataStoreSingletonDelegate()
}

private class MappedPreferenceDataStoreSingletonDelegate : ReadOnlyProperty<Context, (String) -> DataStore<Preferences>> {

    private val dataStoreCache = ConcurrentHashMap<String, DataStore<Preferences>>()
    private val provider: (Context) -> (String) -> DataStore<Preferences> = { context ->
        { key ->
            dataStoreCache.getOrPut(key) {
                PreferenceDataStoreFactory.create {
                    context.applicationContext.preferencesDataStoreFile(key)
                }
            }
        }
    }

    override fun getValue(thisRef: Context, property: KProperty<*>) = provider.invoke(thisRef)
}
