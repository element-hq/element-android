/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui

import android.Manifest
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import im.vector.app.espresso.tools.ScreenshotFailureRule
import im.vector.app.features.MainActivity
import im.vector.app.getString
import im.vector.app.ui.robot.ElementRobot
import im.vector.app.ui.robot.settings.labs.LabFeaturesPreferences
import im.vector.app.ui.robot.withDeveloperMode
import im.vector.lib.strings.CommonStrings
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

    private val elementRobot = ElementRobot(
            LabFeaturesPreferences(
                    InstrumentationRegistry.getInstrumentation()
                            .targetContext
                            .resources
                            .getBoolean(im.vector.app.config.R.bool.settings_labs_new_app_layout_default)
            )
    )

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

        val spaceName = UUID.randomUUID().toString()
        elementRobot.space {
            createSpace(true) {
                createAndCrawl(spaceName)
            }
            val publicSpaceName = UUID.randomUUID().toString()
            createSpace(false) {
                createPublicSpace(publicSpaceName)
            }

            spaceMenu(publicSpaceName) {
                spaceMembers()
                spaceSettings {
                    crawl()
                }
                exploreRooms()

                invitePeople().also { openMenu(publicSpaceName) }
                addRoom().also { openMenu(publicSpaceName) }
                addSpace().also { openMenu(publicSpaceName) }

                leaveSpace()
            }
        }

        // Some instability with the bottomsheet
        // not sure what's the source, maybe the expanded state?
        Thread.sleep(10_000)

        elementRobot.space { selectSpace(spaceName) }

        elementRobot.layoutPreferences {
            crawl()
        }

        elementRobot.roomList {
            crawlTabs()
        }

        elementRobot.withDeveloperMode {
            settings {
                advancedSettings { crawlDeveloperOptions() }
            }
            roomList {
                openRoom(getString(CommonStrings.room_displayname_empty_room)) {
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
        elementRobot.newRoom {
            createNewRoom {
                crawl()
                createRoom(roomName = "thread room") {
                    val message = "Hello This message will be a thread!"
                    postMessage(message)
                    replyToThread(message)
                    viewInRoom(message)
                    openThreadSummaries()
                    selectThreadSummariesFilter()
                }
            }
        }
    }
}
