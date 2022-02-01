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
 * Provides a singleton datastore cache
 * allows for lazily fetching a datastore instance by key to avoid creating multiple stores for the same file
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
                    context.preferencesDataStoreFile(key)
                }
            }
        }
    }

    override fun getValue(thisRef: Context, property: KProperty<*>) = provider.invoke(thisRef)
}
