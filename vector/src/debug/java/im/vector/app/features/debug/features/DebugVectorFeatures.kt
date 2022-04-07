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
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    override fun onboardingVariant(): VectorFeatures.OnboardingVariant {
        return readPreferences().getEnum<VectorFeatures.OnboardingVariant>() ?: vectorFeatures.onboardingVariant()
    }

    override fun isOnboardingAlreadyHaveAccountSplashEnabled(): Boolean = read(DebugFeatureKeys.onboardingAlreadyHaveAnAccount)
            ?: vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()

    override fun isOnboardingSplashCarouselEnabled(): Boolean = read(DebugFeatureKeys.onboardingSplashCarousel)
            ?: vectorFeatures.isOnboardingSplashCarouselEnabled()

    override fun isOnboardingUseCaseEnabled(): Boolean = read(DebugFeatureKeys.onboardingUseCase) ?: vectorFeatures.isOnboardingUseCaseEnabled()

    override fun isOnboardingPersonalizeEnabled(): Boolean = read(DebugFeatureKeys.onboardingPersonalize)
            ?: vectorFeatures.isOnboardingPersonalizeEnabled()

    override fun isOnboardingCombinedRegisterEnabled(): Boolean = read(DebugFeatureKeys.onboardingCombinedRegister)
            ?: vectorFeatures.isOnboardingCombinedRegisterEnabled()

    override fun isLiveLocationEnabled(): Boolean = read(DebugFeatureKeys.liveLocationSharing)
            ?: vectorFeatures.isLiveLocationEnabled()

    fun <T> override(value: T?, key: Preferences.Key<T>) = updatePreferences {
        if (value == null) {
            it.remove(key)
        } else {
            it[key] = value
        }
    }

    fun <T> hasOverride(key: Preferences.Key<T>) = readPreferences().contains(key)

    fun <T : Enum<T>> hasEnumOverride(type: KClass<T>) = readPreferences().containsEnum(type)

    fun <T : Enum<T>> overrideEnum(value: T?, type: KClass<T>) = updatePreferences {
        if (value == null) {
            it.removeEnum(type)
        } else {
            it.putEnum(value, type)
        }
    }

    private fun read(key: Preferences.Key<Boolean>): Boolean? = readPreferences()[key]

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

object DebugFeatureKeys {
    val onboardingAlreadyHaveAnAccount = booleanPreferencesKey("onboarding-already-have-an-account")
    val onboardingSplashCarousel = booleanPreferencesKey("onboarding-splash-carousel")
    val onboardingUseCase = booleanPreferencesKey("onboarding-splash-carousel")
    val onboardingPersonalize = booleanPreferencesKey("onboarding-personalize")
    val onboardingCombinedRegister = booleanPreferencesKey("onboarding-combined-register")
    val liveLocationSharing = booleanPreferencesKey("live-location-sharing")
}
