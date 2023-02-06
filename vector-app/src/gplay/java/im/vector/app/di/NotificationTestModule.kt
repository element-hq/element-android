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
