/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import im.vector.app.features.push.NotificationTroubleshootTestManagerFactory
import im.vector.app.push.fcm.GoogleNotificationTroubleshootTestManagerFactory

@InstallIn(ActivityComponent::class)
@Module
abstract class NotificationTestModule {
    @Binds
    abstract fun bindsNotificationTestFactory(factory: GoogleNotificationTroubleshootTestManagerFactory): NotificationTroubleshootTestManagerFactory
}
