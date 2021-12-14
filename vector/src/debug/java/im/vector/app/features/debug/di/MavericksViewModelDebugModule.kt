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

package im.vector.app.features.debug.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.MavericksViewModelComponent
import im.vector.app.core.di.MavericksViewModelKey
import im.vector.app.features.debug.analytics.DebugAnalyticsViewModel
import im.vector.app.features.debug.settings.DebugPrivateSettingsViewModel

@InstallIn(MavericksViewModelComponent::class)
@Module
interface MavericksViewModelDebugModule {

    @Binds
    @IntoMap
    @MavericksViewModelKey(DebugAnalyticsViewModel::class)
    fun debugAnalyticsViewModelFactory(factory: DebugAnalyticsViewModel.Factory): MavericksAssistedViewModelFactory<*, *>

    @Binds
    @IntoMap
    @MavericksViewModelKey(DebugPrivateSettingsViewModel::class)
    fun debugPrivateSettingsViewModelFactory(factory: DebugPrivateSettingsViewModel.Factory): MavericksAssistedViewModelFactory<*, *>
}
