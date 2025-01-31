/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.GoogleFlavorLegals
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.home.NightlyProxy
import im.vector.app.features.settings.legals.FlavorLegals
import im.vector.app.nightly.FirebaseNightlyProxy
import im.vector.app.push.fcm.GoogleFcmHelper

@InstallIn(SingletonComponent::class)
@Module
abstract class FlavorModule {

    companion object {
        @Provides
        fun provideGuardServiceStarter(): GuardServiceStarter {
            return object : GuardServiceStarter {}
        }
    }

    @Binds
    abstract fun bindsNightlyProxy(nightlyProxy: FirebaseNightlyProxy): NightlyProxy

    @Binds
    abstract fun bindsFcmHelper(fcmHelper: GoogleFcmHelper): FcmHelper

    @Binds
    abstract fun bindsFlavorLegals(legals: GoogleFlavorLegals): FlavorLegals
}
