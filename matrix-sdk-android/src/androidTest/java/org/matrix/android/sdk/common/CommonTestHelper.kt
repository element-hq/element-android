/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.common

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.Observer
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.SyncConfig
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.session.sync.SyncState
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This class exposes methods to be used in common cases
 * Registration, login, Sync, Sending messages...
 */
class CommonTestHelper internal constructor(context: Context) {

    companion object {
        internal fun runSessionTest(context: Context, autoSignoutOnClose: Boolean = true, block: (CommonTestHelper) -> Unit) {
            val testHelper = CommonTestHelper(context)
            return try {
                block(testHelper)
            } finally {
                if (autoSignoutOnClose) {
                    testHelper.cleanUpOpenedSessions()
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        internal fun runSuspendingSessionTest(context: Context, autoSignoutOnClose: Boolean = true, block: suspend CoroutineScope.(CommonTestHelper) -> Unit) {
            val testHelper = CommonTestHelper(context)
            return runTest(dispatchTimeoutMs = TestConstants.timeOutMillis) {
                try {
                    withContext(Dispatchers.Default) {
                        block(testHelper)
                    }
                } finally {
                    if (autoSignoutOnClose) {
                        testHelper.cleanUpOpenedSessions()
                    }
                }
            }
        }

        internal fun runCryptoTest(context: Context, autoSignoutOnClose: Boolean = true, block: (CryptoTestHelper, CommonTestHelper) -> Unit) {
            val testHelper = CommonTestHelper(context)
            val cryptoTestHelper = CryptoTestHelper(testHelper)
            return try {
                block(cryptoTestHelper, testHelper)
            } finally {
                if (autoSignoutOnClose) {
                    testHelper.cleanUpOpenedSessions()
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        internal fun runSuspendingCryptoTest(context: Context, autoSignoutOnClose: Boolean = true, block: suspend CoroutineScope.(CryptoTestHelper, CommonTestHelper) -> Unit) {
            val testHelper = CommonTestHelper(context)
            val cryptoTestHelper = CryptoTestHelper(testHelper)
            return runTest(dispatchTimeoutMs = TestConstants.timeOutMillis) {
                try {
                    withContext(Dispatchers.Default) {
                        block(cryptoTestHelper, testHelper)
                    }
                } finally {
                    if (autoSignoutOnClose) {
                        testHelper.cleanUpOpenedSessions()
                    }
                }
            }
        }
    }

    internal val matrix: TestMatrix
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var accountNumber = 0

    private val trackedSessions = mutableListOf<Session>()

    fun getTestInterceptor(session: Session): MockOkHttpInterceptor? = TestModule.interceptorForSession(session.sessionId) as? MockOkHttpInterceptor

    init {
        var _matrix: TestMatrix? = null
        UiThreadStatement.runOnUiThread {
            _matrix = TestMatrix(
                    context,
                    MatrixConfiguration(
                            applicationFlavor = "TestFlavor",
                            roomDisplayNameFallbackProvider = TestRoomDisplayNameFallbackProvider(),
                            syncConfig = SyncConfig(longPollTimeout = 5_000L),
                    )
            )
        }
        matrix = _matrix!!
    }

    suspend fun createAccount(userNamePrefix: String, testParams: SessionTestParams): Session {
        return createAccount(userNamePrefix, TestConstants.PASSWORD, testParams)
    }

    suspend fun logIntoAccount(userId: String, testParams: SessionTestParams): Session {
        return logIntoAccount(userId, TestConstants.PASSWORD, testParams)
    }

    fun cleanUpOpenedSessions() {
        trackedSessions.forEach {
            runBlockingTest {
                it.signOutService().signOut(true)
            }
        }
        trackedSessions.clear()
    }

    /**
     * Create a homeserver configuration, with Http connection allowed for test
     */
    fun createHomeServerConfig(): HomeServerConnectionConfig {
        return HomeServerConnectionConfig.Builder()
                .withHomeServerUri(Uri.parse(TestConstants.TESTS_HOME_SERVER_URL))
                .build()
    }

    suspend fun syncSession(session: Session, timeout: Long = TestConstants.timeOutMillis * 10) {
        session.syncService().startSync(true)
        val syncLiveData = session.syncService().getSyncStateLive()
        syncLiveData.first(timeout) { session.syncService().hasAlreadySynced() }
    }

    /**
     * This methods clear the cache and waits for initialSync
     *
     * @param session the session to sync
     */
    fun clearCacheAndSync(session: Session, timeout: Long = TestConstants.timeOutMillis) {
        waitWithLatch(timeout) { latch ->
            session.clearCache()
            val syncLiveData = session.syncService().getSyncStateLive()
            val syncObserver = object : Observer<SyncState> {
                override fun onChanged(t: SyncState?) {
                    if (session.syncService().hasAlreadySynced()) {
                        Timber.v("Clear cache and synced")
                        syncLiveData.removeObserver(this)
                        latch.countDown()
                    }
                }
            }
            syncLiveData.observeForever(syncObserver)
            session.syncService().startSync(true)
        }
    }

    /**
     * Sends text messages in a room
     *
     * @param room the room where to send the messages
     * @param message the message to send
     * @param nbOfMessages the number of time the message will be sent
     */
    fun sendTextMessage(room: Room, message: String, nbOfMessages: Int, timeout: Long = TestConstants.timeOutMillis): List<TimelineEvent> {
        return runBlocking { sendTextMessageSuspending(room, message, nbOfMessages, timeout) }
    }

    suspend fun sendTextMessageSuspending(room: Room, message: String, nbOfMessages: Int, timeout: Long = TestConstants.timeOutMillis): List<TimelineEvent> {
        val timeline = room.timelineService().createTimeline(null, TimelineSettings(10))
        timeline.start()
        val sentEvents = sendTextMessagesBatched(timeline, room, message, nbOfMessages, timeout)
        timeline.dispose()
        // Check that all events has been created
        assertEquals("Message number do not match $sentEvents", nbOfMessages.toLong(), sentEvents.size.toLong())
        return sentEvents
    }

    /**
     * Will send nb of messages provided by count parameter but waits every 10 messages to avoid gap in sync
     */
    private suspend fun sendTextMessagesBatched(timeline: Timeline, room: Room, message: String, count: Int, timeout: Long, rootThreadEventId: String? = null): List<TimelineEvent> {
        val sentEvents = ArrayList<TimelineEvent>(count)
        (1 until count + 1)
                .map { "$message #$it" }
                .chunked(10)
                .forEach { batchedMessages ->
                    waitFor(
                            continueWhen = {
                                wrapWithTimeout(timeout) {
                                    suspendCoroutine<Unit> { continuation ->
                                        val timelineListener = object : Timeline.Listener {

                                            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                                                val allSentMessages = snapshot
                                                        .filter { it.root.sendState == SendState.SYNCED }
                                                        .filter { it.root.getClearType() == EventType.MESSAGE }
                                                        .filter { it.root.getClearContent().toModel<MessageContent>()?.body?.startsWith(message) == true }

                                                val hasSyncedAllBatchedMessages = allSentMessages
                                                        .map {
                                                            it.root.getClearContent().toModel<MessageContent>()?.body
                                                        }
                                                        .containsAll(batchedMessages)

                                                if (allSentMessages.size == count) {
                                                    sentEvents.addAll(allSentMessages)
                                                }
                                                if (hasSyncedAllBatchedMessages) {
                                                    timeline.removeListener(this)
                                                    continuation.resume(Unit)
                                                }
                                            }
                                        }
                                        timeline.addListener(timelineListener)
                                    }
                                }
                            },
                            action = {
                                batchedMessages.forEach { formattedMessage ->
                                    if (rootThreadEventId != null) {
                                        room.relationService().replyInThread(
                                                rootThreadEventId = rootThreadEventId,
                                                replyInThreadText = formattedMessage
                                        )
                                    } else {
                                        room.sendService().sendTextMessage(formattedMessage)
                                    }
                                }

                            }
                    )
                }
        return sentEvents
    }

    suspend fun waitForAndAcceptInviteInRoom(otherSession: Session, roomID: String) {
        retryPeriodically {
            val roomSummary = otherSession.getRoomSummary(roomID)
            (roomSummary != null && roomSummary.membership == Membership.INVITE).also {
                if (it) {
                    Log.v("# TEST", "${otherSession.myUserId} can see the invite")
                }
            }
        }

        // not sure why it's taking so long :/
        wrapWithTimeout(90_000) {
            Log.v("#E2E TEST", "${otherSession.myUserId} tries to join room $roomID")
            try {
                otherSession.roomService().joinRoom(roomID)
            } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
                // it's ok we will wait after
            }
        }

        Log.v("#E2E TEST", "${otherSession.myUserId} waiting for join echo ...")
        retryPeriodically {
            val roomSummary = otherSession.getRoomSummary(roomID)
            roomSummary != null && roomSummary.membership == Membership.JOIN
        }
    }

    /**
     * Reply in a thread
     * @param room the room where to send the messages
     * @param message the message to send
     * @param numberOfMessages the number of time the message will be sent
     */
    fun replyInThreadMessage(
            room: Room,
            message: String,
            numberOfMessages: Int,
            rootThreadEventId: String,
            timeout: Long = TestConstants.timeOutMillis
    ): List<TimelineEvent> {
        val timeline = room.timelineService().createTimeline(null, TimelineSettings(10))
        timeline.start()
        val sentEvents = runBlocking { sendTextMessagesBatched(timeline, room, message, numberOfMessages, timeout, rootThreadEventId) }
        timeline.dispose()
        // Check that all events has been created
        assertEquals("Message number do not match $sentEvents", numberOfMessages.toLong(), sentEvents.size.toLong())
        return sentEvents
    }

    // PRIVATE METHODS *****************************************************************************

    private suspend fun createAccount(
            userNamePrefix: String,
            password: String,
            testParams: SessionTestParams
    ): Session {
        val session = createAccountAndSync(
                userNamePrefix + "_" + accountNumber++ + "_" + UUID.randomUUID(),
                password,
                testParams
        )
        assertNotNull(session)
        return session.also {
            // most of the test was created pre-MSC3061 so ensure compatibility
            it.cryptoService().enableShareKeyOnInvite(false)
            trackedSessions.add(session)
        }
    }

    suspend fun logIntoAccount(
            userId: String,
            password: String,
            testParams: SessionTestParams
    ): Session {
        val session = logAccountAndSync(userId, password, testParams)
        assertNotNull(session)
        return session.also {
            trackedSessions.add(session)
        }
    }

    private suspend fun createAccountAndSync(
            userName: String,
            password: String,
            sessionTestParams: SessionTestParams
    ): Session {
        val hs = createHomeServerConfig()

        wrapWithTimeout(TestConstants.timeOutMillis) {
            matrix.authenticationService.getLoginFlow(hs)
        }

        wrapWithTimeout(60_000L) {
            matrix.authenticationService
                    .getRegistrationWizard()
                    .createAccount(userName, password, null)
        }

        // Perform dummy step
        val registrationResult = wrapWithTimeout(timeout = 60_000) {
            matrix.authenticationService
                    .getRegistrationWizard()
                    .dummy()
        }

        assertTrue(registrationResult is RegistrationResult.Success)
        val session = (registrationResult as RegistrationResult.Success).session
        session.open()
        if (sessionTestParams.withInitialSync) {
            syncSession(session, 120_000)
        }
        return session
    }

    private suspend fun logAccountAndSync(userName: String, password: String, sessionTestParams: SessionTestParams): Session {
        val hs = createHomeServerConfig()

        matrix.authenticationService.getLoginFlow(hs)

        val session = matrix.authenticationService
                .getLoginWizard()
                .login(userName, password, "myDevice")
        session.open()
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
    fun logAccountWithError(
            userName: String,
            password: String
    ): Throwable {
        val hs = createHomeServerConfig()

        runBlockingTest {
            matrix.authenticationService.getLoginFlow(hs)
        }

        var requestFailure: Throwable? = null
        runBlockingTest {
            try {
                matrix.authenticationService
                        .getLoginWizard()
                        .login(userName, password, "myDevice")
            } catch (failure: Throwable) {
                requestFailure = failure
            }
        }

        assertNotNull(requestFailure)
        return requestFailure!!
    }

    fun createEventListener(latch: CountDownLatch, predicate: (List<TimelineEvent>) -> Boolean): Timeline.Listener {
        return object : Timeline.Listener {

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
    fun await(latch: CountDownLatch, timeout: Long? = TestConstants.timeOutMillis, job: Job? = null) {
        assertTrue(
                "Timed out after " + timeout + "ms waiting for something to happen. See stacktrace for cause.",
                latch.await(timeout ?: TestConstants.timeOutMillis, TimeUnit.MILLISECONDS).also {
                    if (!it) {
                        // cancel job on timeout
                        job?.cancel("Await timeout")
                    }
                }
        )
    }

    suspend fun retryPeriodically(timeout: Long = TestConstants.timeOutMillis, predicate: suspend () -> Boolean) {
        wrapWithTimeout(timeout) {
            while (!predicate()) {
                runBlocking { delay(500) }
            }
        }
    }

    suspend fun retryPeriodicallyWithLatch(latch: CountDownLatch, condition: (() -> Boolean)) {
        while (true) {
            try {
                delay(1000)
            } catch (ex: CancellationException) {
                // the job was canceled, just stop
                return
            }
            if (condition()) {
                latch.countDown()
                return
            }
        }
    }

    fun <T> launch(block: suspend () -> T): T {
        return runBlocking {
            block()
        }
    }

    fun waitWithLatch(timeout: Long? = TestConstants.timeOutMillis, dispatcher: CoroutineDispatcher = Dispatchers.Main, block: suspend (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        val job = coroutineScope.launch(dispatcher) {
            block(latch)
        }
        await(latch, timeout, job)
    }

    fun <T> runBlockingTest(timeout: Long = TestConstants.timeOutMillis, block: suspend () -> T): T {
        return runBlocking {
            wrapWithTimeout(timeout) {
                block()
            }
        }
    }

    // Transform a method with a MatrixCallback to a synchronous method
    inline fun <reified T> doSync(timeout: Long? = TestConstants.timeOutMillis, block: (MatrixCallback<T>) -> Unit): T {
        val lock = CountDownLatch(1)
        var result: T? = null

        val callback = object : TestMatrixCallback<T>(lock) {
            override fun onSuccess(data: T) {
                result = data
                super.onSuccess(data)
            }
        }

        block.invoke(callback)

        await(lock, timeout)

        assertNotNull(result)
        return result!!
    }

    suspend fun <T> doSyncSuspending(timeout: Long = TestConstants.timeOutMillis, block: (MatrixCallback<T>) -> Unit): T {
        return wrapWithTimeout(timeout) {
            suspendCoroutine { continuation ->
                val callback = object : MatrixCallback<T> {
                    override fun onSuccess(data: T) {
                        continuation.resume(data)
                    }
                }
                block(callback)
            }
        }
    }

    /**
     * Clear all provided sessions
     */
    suspend fun Iterable<Session>.signOutAndClose() = forEach { signOutAndClose(it) }

    suspend fun signOutAndClose(session: Session) {
        trackedSessions.remove(session)
        wrapWithTimeout(timeout = 60_000L) {
            session.signOutService().signOut(true)
        }
        // no need signout will close
        // session.close()
    }
}

fun List<TimelineEvent>.checkSendOrder(baseTextMessage: String, numberOfMessages: Int, startIndex: Int): Boolean {
    return drop(startIndex)
            .take(numberOfMessages)
            .foldRightIndexed(true) { index, timelineEvent, acc ->
                val body = timelineEvent.root.content.toModel<MessageContent>()?.body
                val currentMessageSuffix = numberOfMessages - index
                acc && (body == null || body.startsWith(baseTextMessage) && body.endsWith("#$currentMessageSuffix"))
            }
}
