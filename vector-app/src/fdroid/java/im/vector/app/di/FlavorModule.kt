/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.DefaultAppNameProvider
import im.vector.app.core.resources.DefaultLocaleProvider
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.fdroid.service.FDroidGuardServiceStarter
import im.vector.app.features.home.NightlyProxy
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.legals.FlavorLegals
import im.vector.app.push.fcm.FdroidFcmHelper

@InstallIn(SingletonComponent::class)
@Module
abstract class FlavorModule {

    companion object {
        @Provides
        fun provideGuardServiceStarter(preferences: VectorPreferences, appContext: Context): GuardServiceStarter {
            return FDroidGuardServiceStarter(preferences, appContext)
        }

        @Provides
        fun provideNightlyProxy() = object : NightlyProxy {
            override fun canDisplayPopup() = false
            override fun isNightlyBuild() = false
            override fun updateApplication() = Unit
        }

        @Provides
        fun providesFlavorLegals() = object : FlavorLegals {
            override fun hasThirdPartyNotices() = false

            override fun navigateToThirdPartyNotices(context: Context) {
                // no op
            }
        }
    }

    @Binds
    abstract fun bindsFcmHelper(fcmHelper: FdroidFcmHelper): FcmHelper

    @Binds
    abstract fun bindsLocaleProvider(localeProvider: DefaultLocaleProvider): LocaleProvider

    @Binds
    abstract fun bindsAppNameProvider(appNameProvider: DefaultAppNameProvider): AppNameProvider
}
