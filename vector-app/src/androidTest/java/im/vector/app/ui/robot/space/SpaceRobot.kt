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

package im.vector.app.ui.robot.space

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.internal.viewaction.ClickChildAction
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import im.vector.app.ui.robot.settings.labs.LabFeaturesPreferences
import im.vector.lib.strings.CommonStrings
import org.hamcrest.Matchers

class SpaceRobot(private val labsPreferences: LabFeaturesPreferences) {
    private val features: VectorFeatures = DefaultVectorFeatures()

    fun createSpace(isFirstSpace: Boolean, block: SpaceCreateRobot.() -> Unit) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.newLayoutOpenSpacesButton)
            if (isFirstSpace) {
                waitUntilDialogVisible(ViewMatchers.withId(R.id.spaces_empty_group))
                clickOn(R.id.spaces_empty_button)
            } else {
                waitUntilDialogVisible(ViewMatchers.withId(R.id.groupListView))
                Espresso.onView(ViewMatchers.withId(R.id.groupListView))
                        .perform(
                                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                        ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.plus)),
                                        click()
                                ).atPosition(0)
                        )
            }
        } else {
            openDrawer()
            clickOn(CommonStrings.create_space)
        }
        block(SpaceCreateRobot())
    }

    fun spaceMenu(spaceName: String, block: SpaceMenuRobot.() -> Unit) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.newLayoutOpenSpacesButton)
            waitUntilDialogVisible(ViewMatchers.withId(R.id.groupListView))
        } else {
            openDrawer()
        }
        with(SpaceMenuRobot()) {
            openMenu(spaceName)
            block()
        }
    }

    fun openMenu(spaceName: String) {
        waitUntilViewVisible(ViewMatchers.withId(R.id.groupListView))
        if (labsPreferences.isNewAppLayoutEnabled) {
            Espresso.onView(ViewMatchers.withId(R.id.groupListView))
                    .perform(
                            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                    ViewMatchers.hasDescendant(Matchers.allOf(ViewMatchers.withId(R.id.name), ViewMatchers.withText(spaceName))),
                                    longClick()
                            ).atPosition(0)
                    )
        } else {
            Espresso.onView(ViewMatchers.withId(R.id.groupListView))
                    .perform(
                            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                    ViewMatchers.hasDescendant(Matchers.allOf(ViewMatchers.withId(R.id.groupNameView), ViewMatchers.withText(spaceName))),
                                    ClickChildAction.clickChildWithId(R.id.groupTmpLeave)
                            ).atPosition(0)
                    )
        }

        waitUntilDialogVisible(ViewMatchers.withId(R.id.spaceNameView))
    }

    fun selectSpace(spaceName: String) {
        if (!labsPreferences.isNewAppLayoutEnabled) {
            openDrawer()
            waitUntilViewVisible(ViewMatchers.withId(R.id.groupListView))
        }
        clickOn(spaceName)
    }
}
