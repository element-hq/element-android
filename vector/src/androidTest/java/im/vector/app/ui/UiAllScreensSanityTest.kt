/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.ui

import android.Manifest
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import im.vector.app.R
import im.vector.app.espresso.tools.ScreenshotFailureRule
import im.vector.app.features.MainActivity
import im.vector.app.getString
import im.vector.app.ui.robot.ElementRobot
import im.vector.app.ui.robot.settings.labs.LabFeature
import im.vector.app.ui.robot.withDeveloperMode
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * This test aim to open every possible screen of the application
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UiAllScreensSanityTest {

    @get:Rule
    val testRule = RuleChain
            .outerRule(ActivityScenarioRule(MainActivity::class.java))
            .around(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(ScreenshotFailureRule())

    private val elementRobot = ElementRobot()

    // Last passing:
    // 2020-11-09
    // 2020-12-16 After ViewBinding huge change
    // 2021-04-08 Testing 429 change
    @Test
    fun allScreensTest() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)

        elementRobot.onboarding {
            crawl()
        }

        // Create an account
        val userId = "UiTest_" + UUID.randomUUID().toString()
        elementRobot.signUp(userId)

        elementRobot.settings {
            general { crawl() }
            notifications { crawl() }
            preferences { crawl() }
            voiceAndVideo()
            ignoredUsers()
            securityAndPrivacy { crawl() }
            labs()
            advancedSettings { crawl() }
            helpAndAbout { crawl() }
            legals { crawl() }
        }

        elementRobot.newDirectMessage {
            verifyQrCodeButton()
            verifyInviteFriendsButton()
        }

        elementRobot.newRoom {
            createNewRoom {
                crawl()
                createRoom {
                    val message = "Hello world!"
                    postMessage(message)
                    crawl()
                    crawlMessage(message)
                    openSettings { crawl() }
                }
            }
        }

        testThreadScreens()

        elementRobot.space {
            createSpace {
                crawl()
            }
            val spaceName = UUID.randomUUID().toString()
            createSpace {
                createPublicSpace(spaceName)
            }

            spaceMenu(spaceName) {
                spaceMembers()
                spaceSettings {
                    crawl()
                }
                exploreRooms()

                invitePeople().also { openMenu(spaceName) }
                addRoom().also { openMenu(spaceName) }
                addSpace().also { openMenu(spaceName) }

                leaveSpace()
            }
        }

        elementRobot.withDeveloperMode {
            settings {
                advancedSettings { crawlDeveloperOptions() }
            }
            roomList {
                openRoom(getString(R.string.room_displayname_empty_room)) {
                    val message = "Test view source"
                    postMessage(message)
                    openMessageMenu(message) {
                        viewSource()
                    }
                }
            }
        }

        elementRobot.roomList {
            verifyCreatedRoom()
        }

        elementRobot.signout(expectSignOutWarning = true)

        // Login again on the same account
        elementRobot.login(userId)
        elementRobot.dismissVerificationIfPresent()
        // TODO Deactivate account instead of logout?
        elementRobot.signout(expectSignOutWarning = false)
    }

    /**
     * Testing multiple threads screens
     */
    private fun testThreadScreens() {
        elementRobot.toggleLabFeature(LabFeature.THREAD_MESSAGES)
        elementRobot.newRoom {
            createNewRoom {
                crawl()
                createRoom {
                    val message = "Hello This message will be a thread!"
                    postMessage(message)
                    replyToThread(message)
                    viewInRoom(message)
                    openThreadSummaries()
                    selectThreadSummariesFilter()
                }
            }
        }
        elementRobot.toggleLabFeature(LabFeature.THREAD_MESSAGES)
    }
}
