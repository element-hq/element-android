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

package im.vector.app.screenshot

import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.launcher.MavericksLauncherMockActivity
import com.airbnb.mvrx.mocking.MockableMavericksView
import com.airbnb.mvrx.mocking.getMockVariants
import com.airbnb.mvrx.mocking.mockSingleViewModel
import com.karumi.shot.ActivityScenarioUtils.waitForActivity
import com.karumi.shot.ScreenshotTest
import im.vector.app.features.roomprofile.RoomProfileFragment
import im.vector.app.features.roomprofile.mocks.mockRoomProfileArgs
import im.vector.app.features.roomprofile.mocks.mockRoomProfileViewState
import org.junit.Test
import kotlin.reflect.KClass

class MvRxScreenshotTest : ScreenshotTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultRoomProfileFragmentScreenshotTest() {
        val mock = MockRoomProfileFragment::class.getMock("Default state")

        val scenario = ActivityScenario.launch<MavericksLauncherMockActivity>(
                MavericksLauncherMockActivity.intent(context, mock)
        )

        compareScreenshot(scenario.waitForActivity())
    }

    @Test
    fun anotherRoomProfileFragmentScreenshotTest() {
        val mock = MockRoomProfileFragment::class.getMock("my-state")

        val scenario = ActivityScenario.launch<MavericksLauncherMockActivity>(
                MavericksLauncherMockActivity.intent(context, mock)
        )

        compareScreenshot(scenario.waitForActivity())
    }
}

fun <T : MockableMavericksView> KClass<T>.getMock(name: String) = getMockVariants(this.java)!!.first { it.mock.name == name }

class MockRoomProfileFragment : RoomProfileFragment(), MockableMavericksView {

    override fun provideMocks() = mockSingleViewModel(
            viewModelReference = MockRoomProfileFragment::roomProfileViewModel,
            defaultState = mockRoomProfileViewState,
            defaultArgs = mockRoomProfileArgs,
    ) {
        state("my-state") {
            val summary = (mockRoomProfileViewState.roomSummary as Success)().copy(
                    displayName = "another state"
            )
            mockRoomProfileViewState.copy(
                    roomSummary = Success(summary)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        registerMockPrinter()
        super.onCreate(savedInstanceState)
    }
}
