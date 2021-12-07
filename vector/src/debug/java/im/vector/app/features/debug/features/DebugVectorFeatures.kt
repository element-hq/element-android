/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.debug.features

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "debug_features")

class DebugVectorFeatures(
        context: Context,
        private val vectorFeatures: DefaultVectorFeatures
) : VectorFeatures {

    private val dataStore = context.dataStore

    override fun loginVersion(): VectorFeatures.LoginVersion {
        return readPreferences().getEnum<VectorFeatures.LoginVersion>() ?: vectorFeatures.loginVersion()
    }

    fun <T : Enum<T>> hasEnumOverride(type: KClass<T>) = readPreferences().containsEnum(type)

    fun <T : Enum<T>> overrideEnum(value: T?, type: KClass<T>) {
        if (value == null) {
            updatePreferences { it.removeEnum(type) }
        } else {
            updatePreferences { it.putEnum(value, type) }
        }
    }

    private fun readPreferences() = runBlocking { dataStore.data.first() }

    private fun updatePreferences(block: (MutablePreferences) -> Unit) = runBlocking {
        dataStore.edit { block(it) }
    }
}

private fun <T : Enum<T>> MutablePreferences.removeEnum(type: KClass<T>) {
    remove(enumPreferencesKey(type))
}

private fun <T : Enum<T>> Preferences.containsEnum(type: KClass<T>) = contains(enumPreferencesKey(type))

private fun <T : Enum<T>> MutablePreferences.putEnum(value: T, type: KClass<T>) {
    this[enumPreferencesKey(type)] = value.name
}

private inline fun <reified T : Enum<T>> Preferences.getEnum(): T? {
    return get(enumPreferencesKey<T>())?.let { enumValueOf<T>(it) }
}

private inline fun <reified T : Enum<T>> enumPreferencesKey() = enumPreferencesKey(T::class)

private fun <T : Enum<T>> enumPreferencesKey(type: KClass<T>) = stringPreferencesKey("enum-${type.simpleName}")
