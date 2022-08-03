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
import androidx.test.rule.GrantPermissionRule
import im.vector.app.espresso.tools.ScreenshotFailureRule
import im.vector.app.features.MainActivity
import im.vector.app.ui.robot.ElementRobot
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
    val testRule: RuleChain = RuleChain
            .outerRule(ActivityScenarioRule(MainActivity::class.java))
            .around(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(ScreenshotFailureRule())

    private val elementRobot = ElementRobot()

    private val client = OkHttpClient()

    @Test
    fun trafficLightCycle() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)

        // keep the same UUID during each run; otherwise it should be entirely random.
        val uuid = UUID.randomUUID().toString()
        val trafficLightServer = "http://10.0.2.2:5000"
        val postURL = "$trafficLightServer/client/$uuid/respond"
        val pollURL = "$trafficLightServer/client/$uuid/poll"
        val registerURL = "$trafficLightServer/client/$uuid/register"

        register(registerURL)
        while (true) {
            var pollResponse = poll(pollURL)

            var action = pollResponse.get("action")
            if (action == "login") {
                val data = pollResponse.getJSONObject("data")
                val user = data.getString("username")
                val pass = data.getString("password")
                val homeserverURL = data.getJSONObject("homeserver_url").getString("local_docker")
                elementRobot.login(homeserverURL, user, pass)

                // TODO: Determine how to avoid these 60s delays before prompt arrives.
                // TODO (alt): Determine how to wait for the prompt to be visible before continuing instead of explicit wait.
                Thread.sleep(20000); // try to wait for cross signing to have kicked in...
                println("Waited 20s")
                Thread.sleep(20000);
                println("Waited 40s")
                Thread.sleep(20000);
                println("Waited 60s")
                post(postURL, "{\"response\": \"loggedin\"}")
            }
            if (action == "register") {
                val data = pollResponse.getJSONObject("data")
                val user = data.getString("username")
                val pass = data.getString("password")
                val homeserverURL = data.getJSONObject("homeserver_url").getString("local_docker")
                elementRobot.register(homeserverURL, user, pass)

                sleep(20000); // try to wait for cross signing to have kicked in...
                println("Waited 20s")
                sleep(20000);
                println("Waited 40s")
                sleep(20000);
                println("Waited 60s")

                post(postURL, "{\"response\": \"registered\"}")
            }
            if (action == "idle") {
                val data = pollResponse.getJSONObject("data")
                val delay = data.getLong("delay")
                sleep(delay)
            }
            // client will be told to start OR accept cross signing request
            if (action == "start_crosssign") {
                elementRobot.startVerification()
                post(postURL, "{\"response\": \"started_crosssign\"}")
            }
            if (action == "accept_crosssign") {
                elementRobot.acceptVerification()
                post(postURL, "{\"response\": \"accepted_crosssign\"}")
            }
            // Both clients will be told to verify the cross sign
            if (action == "verify_crosssign_emoji") {
                elementRobot.completeVerification()
                post(postURL, "{\"response\": \"verified_crosssign\"}")
            }
            // exit test
            if (action == "exit") {
                break
            }
            sleep(500) // provide a minimum delay between polls, prevent tightly spinning loops
        }
    }

    private val JSON: MediaType = "application/json".toMediaType()

    private fun post(url: String, json: String): String {
        val body: RequestBody = json.toRequestBody(JSON)
        val request: Request = Request.Builder()
                .url(url)
                .post(body)
                .build()
        client.newCall(request).execute().use { response -> return response.body!!.string() }
    }

    private fun poll(url: String): JSONObject {
        val request: Request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).execute().use { response -> return JSONObject(response.body!!.string()) }
    }

    private fun register(url: String): JSONObject {
        val json = "{ \"type\": \"element-android\", \"version\":\"0.whatever.1\"}"

        val body: RequestBody = json.toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).execute().use { response -> return JSONObject(response.body!!.string()) }
    }
}


