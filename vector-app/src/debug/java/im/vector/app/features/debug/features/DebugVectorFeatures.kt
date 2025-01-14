/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.config.OnboardingVariant
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

    override fun onboardingVariant(): OnboardingVariant {
        return readPreferences().getEnum<OnboardingVariant>() ?: vectorFeatures.onboardingVariant()
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

    override fun isOnboardingCombinedLoginEnabled(): Boolean = read(DebugFeatureKeys.onboardingCombinedLogin)
            ?: vectorFeatures.isOnboardingCombinedLoginEnabled()

    override fun allowExternalUnifiedPushDistributors(): Boolean = read(DebugFeatureKeys.allowExternalUnifiedPushDistributors)
            ?: vectorFeatures.allowExternalUnifiedPushDistributors()

    override fun isScreenSharingEnabled(): Boolean = read(DebugFeatureKeys.screenSharing)
            ?: vectorFeatures.isScreenSharingEnabled()

    override fun isLocationSharingEnabled(): Boolean = read(DebugFeatureKeys.liveLocationSharing)
            ?: vectorFeatures.isLocationSharingEnabled()

    override fun forceUsageOfOpusEncoder(): Boolean = read(DebugFeatureKeys.forceUsageOfOpusEncoder)
            ?: vectorFeatures.forceUsageOfOpusEncoder()

    override fun isNewAppLayoutFeatureEnabled(): Boolean = read(DebugFeatureKeys.newAppLayoutEnabled)
            ?: vectorFeatures.isNewAppLayoutFeatureEnabled()

    override fun isVoiceBroadcastEnabled(): Boolean = read(DebugFeatureKeys.voiceBroadcastEnabled)
            ?: vectorFeatures.isVoiceBroadcastEnabled()

    override fun isUnverifiedSessionsAlertEnabled(): Boolean = read(DebugFeatureKeys.unverifiedSessionsAlertEnabled)
            ?: vectorFeatures.isUnverifiedSessionsAlertEnabled()

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
    val onboardingUseCase = booleanPreferencesKey("onboarding-use-case")
    val onboardingPersonalize = booleanPreferencesKey("onboarding-personalize")
    val onboardingCombinedRegister = booleanPreferencesKey("onboarding-combined-register")
    val onboardingCombinedLogin = booleanPreferencesKey("onboarding-combined-login")
    val allowExternalUnifiedPushDistributors = booleanPreferencesKey("allow-external-unified-push-distributors")
    val liveLocationSharing = booleanPreferencesKey("live-location-sharing")
    val screenSharing = booleanPreferencesKey("screen-sharing")
    val forceUsageOfOpusEncoder = booleanPreferencesKey("force-usage-of-opus-encoder")
    val newAppLayoutEnabled = booleanPreferencesKey("new-app-layout-enabled")
    val voiceBroadcastEnabled = booleanPreferencesKey("voice-broadcast-enabled")
    val unverifiedSessionsAlertEnabled = booleanPreferencesKey("unverified-sessions-alert-enabled")
}
