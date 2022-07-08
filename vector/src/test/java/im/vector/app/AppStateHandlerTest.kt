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

package im.vector.app

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.ui.UiStateRepository
import io.mockk.mockk

internal class AppStateHandlerTest {

    private val sessionDataSource: ActiveSessionDataSource = mockk()
    private val uiStateRepository: UiStateRepository = mockk()
    private val activeSessionHolder: ActiveSessionHolder = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk()

    private val appStateHandlerImpl = AppStateHandlerImpl(
            sessionDataSource,
            uiStateRepository,
            activeSessionHolder,
            analyticsTracker,
    )



}
