/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.common

import android.content.Context
import android.net.Uri
import androidx.lifecycle.Observer
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.LoginFlowResult
import im.vector.matrix.android.api.auth.registration.RegistrationResult
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.api.session.sync.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * This class exposes methods to be used in common cases
 * Registration, login, Sync, Sending messages...
 */
class CommonTestHelper(context: Context) {

    val matrix: Matrix

    init {
        Matrix.initialize(context, MatrixConfiguration("TestFlavor"))

        matrix = Matrix.getInstance(context)
    }

    fun createAccount(userNamePrefix: String, testParams: SessionTestParams): Session {
        return createAccount(userNamePrefix, TestConstants.PASSWORD, testParams)
    }

    fun logIntoAccount(userId: String, testParams: SessionTestParams): Session {
        return logIntoAccount(userId, TestConstants.PASSWORD, testParams)
    }

    /**
     * Create a Home server configuration, with Http connection allowed for test
     */
    fun createHomeServerConfig(): HomeServerConnectionConfig {
        return HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(TestConstants.TESTS_HOME_SERVER_URL))
                .build()
    }

    /**
     * This methods init the event stream and check for initial sync
     *
     * @param session    the session to sync
     */
    fun syncSession(session: Session) {
        val lock = CountDownLatch(1)

        session.open()
        session.startSync(true)

        val syncLiveData = runBlocking(Dispatchers.Main) {
            session.getSyncStateLive()
        }
        val syncObserver = object : Observer<SyncState> {
            override fun onChanged(t: SyncState?) {
                if (session.hasAlreadySynced()) {
                    lock.countDown()
                    syncLiveData.removeObserver(this)
                }
            }
        }
        GlobalScope.launch(Dispatchers.Main) { syncLiveData.observeForever(syncObserver) }

        await(lock)
    }

    /**
     * Sends text messages in a room
     *
     * @param room         the room where to send the messages
     * @param message      the message to send
     * @param nbOfMessages the number of time the message will be sent
     */
    fun sendTextMessage(room: Room, message: String, nbOfMessages: Int): List<TimelineEvent> {
        val sentEvents = ArrayList<TimelineEvent>(nbOfMessages)
        val latch = CountDownLatch(1)
        val timelineListener = object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
            }

            override fun onNewTimelineEvents(eventIds: List<String>) {
                // noop
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                val newMessages = snapshot
                        .filter { it.root.sendState == SendState.SYNCED }
                        .filter { it.root.getClearType() == EventType.MESSAGE }
                        .filter { it.root.getClearContent().toModel<MessageContent>()?.body?.startsWith(message) == true }

                if (newMessages.size == nbOfMessages) {
                    sentEvents.addAll(newMessages)
                    latch.countDown()
                }
            }
        }
        val timeline = room.createTimeline(null, TimelineSettings(10))
        timeline.start()
        timeline.addListener(timelineListener)
        for (i in 0 until nbOfMessages) {
            room.sendTextMessage(message + " #" + (i + 1))
        }
        // Wait 3 second more per message
        await(latch, timeout = TestConstants.timeOutMillis + 3_000L * nbOfMessages)
        timeline.removeListener(timelineListener)
        timeline.dispose()

        // Check that all events has been created
        assertEquals(nbOfMessages.toLong(), sentEvents.size.toLong())

        return sentEvents
    }

    // PRIVATE METHODS *****************************************************************************

    /**
     * Creates a unique account
     *
     * @param userNamePrefix the user name prefix
     * @param password       the password
     * @param testParams     test params about the session
     * @return the session associated with the newly created account
     */
    private fun createAccount(userNamePrefix: String,
                              password: String,
                              testParams: SessionTestParams): Session {
        val session = createAccountAndSync(
                userNamePrefix + "_" + System.currentTimeMillis() + UUID.randomUUID(),
                password,
                testParams
        )
        assertNotNull(session)
        return session
    }

    /**
     * Logs into an existing account
     *
     * @param userId     the userId to log in
     * @param password   the password to log in
     * @param testParams test params about the session
     * @return the session associated with the existing account
     */
    fun logIntoAccount(userId: String,
                       password: String,
                       testParams: SessionTestParams): Session {
        val session = logAccountAndSync(userId, password, testParams)
        assertNotNull(session)
        return session
    }

    /**
     * Create an account and a dedicated session
     *
     * @param userName          the account username
     * @param password          the password
     * @param sessionTestParams parameters for the test
     */
    private fun createAccountAndSync(userName: String,
                                     password: String,
                                     sessionTestParams: SessionTestParams): Session {
        val hs = createHomeServerConfig()

        doSync<LoginFlowResult> {
            matrix.authenticationService
                    .getLoginFlow(hs, it)
        }

        doSync<RegistrationResult> {
            matrix.authenticationService
                    .getRegistrationWizard()
                    .createAccount(userName, password, null, it)
        }

        // Preform dummy step
        val registrationResult = doSync<RegistrationResult> {
            matrix.authenticationService
                    .getRegistrationWizard()
                    .dummy(it)
        }

        assertTrue(registrationResult is RegistrationResult.Success)
        val session = (registrationResult as RegistrationResult.Success).session
        if (sessionTestParams.withInitialSync) {
            syncSession(session)
        }

        return session
    }

    /**
     * Start an account login
     *
     * @param userName          the account username
     * @param password          the password
     * @param sessionTestParams session test params
     */
    private fun logAccountAndSync(userName: String,
                                  password: String,
                                  sessionTestParams: SessionTestParams): Session {
        val hs = createHomeServerConfig()

        doSync<LoginFlowResult> {
            matrix.authenticationService
                    .getLoginFlow(hs, it)
        }

        val session = doSync<Session> {
            matrix.authenticationService
                    .getLoginWizard()
                    .login(userName, password, "myDevice", it)
        }

        if (sessionTestParams.withInitialSync) {
            syncSession(session)
        }

        return session
    }

    /**
     * Log into the account and expect an error
     *
     * @param userName the account username
     * @param password the password
     */
    fun logAccountWithError(userName: String,
                            password: String): Throwable {
        val hs = createHomeServerConfig()

        doSync<LoginFlowResult> {
            matrix.authenticationService
                    .getLoginFlow(hs, it)
        }

        var requestFailure: Throwable? = null
        waitWithLatch { latch ->
            matrix.authenticationService
                    .getLoginWizard()
                    .login(userName, password, "myDevice", object : TestMatrixCallback<Session>(latch, onlySuccessful = false) {
                        override fun onFailure(failure: Throwable) {
                            requestFailure = failure
                            super.onFailure(failure)
                        }
                    })
        }

        assertNotNull(requestFailure)
        return requestFailure!!
    }

    fun createEventListener(latch: CountDownLatch, predicate: (List<TimelineEvent>) -> Boolean): Timeline.Listener {
        return object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
                // noop
            }

            override fun onNewTimelineEvents(eventIds: List<String>) {
                // noop
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                if (predicate(snapshot)) {
                    latch.countDown()
                }
            }
        }
    }

    /**
     * Await for a latch and ensure the result is true
     *
     * @param latch
     * @throws InterruptedException
     */
    fun await(latch: CountDownLatch, timeout: Long? = TestConstants.timeOutMillis) {
        assertTrue(latch.await(timeout ?: TestConstants.timeOutMillis, TimeUnit.MILLISECONDS))
    }

    fun retryPeriodicallyWithLatch(latch: CountDownLatch, condition: (() -> Boolean)) {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                if (condition()) {
                    latch.countDown()
                    return@launch
                }
            }
        }
    }

    fun waitWithLatch(timeout: Long? = TestConstants.timeOutMillis, block: (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        block(latch)
        await(latch, timeout)
    }

    // Transform a method with a MatrixCallback to a synchronous method
    inline fun <reified T> doSync(block: (MatrixCallback<T>) -> Unit): T {
        val lock = CountDownLatch(1)
        var result: T? = null

        val callback = object : TestMatrixCallback<T>(lock) {
            override fun onSuccess(data: T) {
                result = data
                super.onSuccess(data)
            }
        }

        block.invoke(callback)

        await(lock)

        assertNotNull(result)
        return result!!
    }

    /**
     * Clear all provided sessions
     */
    fun Iterable<Session>.signOutAndClose() = forEach { signOutAndClose(it) }

    fun signOutAndClose(session: Session) {
        doSync<Unit> { session.signOut(true, it) }
        session.close()
    }
}
