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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * TrafficLight client is a UI test that receives requests from a TrafficLight server and fufils them on the UI
 */
@RunWith(AndroidJUnit4::class)
class TrafficLightClientTest {

    @get:Rule
    val testRule = RuleChain
            .outerRule(ActivityScenarioRule(MainActivity::class.java))
            .around(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(ScreenshotFailureRule())

    private val elementRobot = ElementRobot()

    val client = OkHttpClient()

    val JSON: MediaType = "application/json".toMediaType()

    fun post(url: String, json: String): String {
        val body: RequestBody = json.toRequestBody(JSON)
        val request: Request = Request.Builder()
                .url(url)
                .post(body)
                .build()
        client.newCall(request).execute().use { response -> return response.body!!.string() }
    }

    fun poll(url: String): JSONObject {
        val request: Request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).execute().use { response -> return JSONObject(response.body!!.string()) }
    }

    fun register(url: String): JSONObject {
        val json = "{ \"type\": \"element-android\", \"version\":\"0.whatever.1\"}"

        val body: RequestBody = json.toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).execute().use { response -> return JSONObject(response.body!!.string()) }
    }
    @Test
    fun trafficLightCycle() {
    IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)

    // keep the same UUID during each run; otherwise it should be entirely random.
    val uuid = UUID.randomUUID().toString()
    val posturl = "http://10.0.2.2:5000/client/" + uuid + "/respond"
    val pollurl = "http://10.0.2.2:5000/client/" + uuid + "/poll"

    val registerurl = "http://10.0.2.2:5000/client/" + uuid + "/register"
    register(registerurl)
    while (true) {
        var poll_response = poll(pollurl)

        var action = poll_response.get("action")
        if (action == "login") {
            val data = poll_response.getJSONObject("data")
            val user = data.getString("username")
            val pass = data.getString("password")
            elementRobot.login(user, pass)

            // DO NOT JUDGE THIS TEST SUITE BUT...
            Thread.sleep(60000); // try to wait for cross signing to have kicked in...
            post(posturl, "{\"response\": \"loggedin\"}")
        }
        if (action == "register") {
            val data = poll_response.getJSONObject("data")
            val user = data.getString("username")
            val pass = data.getString("password")
            elementRobot.register(user, pass)

            Thread.sleep(20000); // try to wait for cross signing to have kicked in...
            System.out.println("Waited 20s")
            Thread.sleep(20000); // try to wait for cross signing to have kicked in...
            System.out.println("Waited 40s")
            Thread.sleep(20000); // try to wait for cross signing to have kicked in...
            System.out.println("Waited 60s")

            post(posturl, "{\"response\": \"registered\"}")
        }
        if (action == "idle") {
            sleep(5000)
        }
        // client will be told to start OR accept cross signing request
        if (action == "start_crosssign") {
            elementRobot.startVerification()
            post(posturl, "{\"response\": \"started_crosssign\"}")
        }
        if (action == "accept_crosssign") {
            elementRobot.acceptVerification()
            post(posturl, "{\"response\": \"accepted_crosssign\"}")
        }
        // Both clients will be told to verify the cross sign
        if (action == "verify_crosssign_emoji") {
            elementRobot.completeVerification()
            post(posturl, "{\"response\": \"verified_crosssign\"}")
        }
        // exit test and reset for next adventure
        if (action == "exit") {
            break
        }
        sleep(1000)
    }
}
}


