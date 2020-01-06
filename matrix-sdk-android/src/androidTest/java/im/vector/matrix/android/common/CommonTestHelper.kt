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
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.LoginFlowResult
import im.vector.matrix.android.api.auth.registration.RegistrationResult
import im.vector.matrix.android.api.session.Session
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.util.*
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
        //val lock = CountDownLatch(1)

        // val observer = androidx.lifecycle.Observer<SyncState> { syncState ->
        //     if (syncState is SyncState.Idle) {
        //         lock.countDown()
        //     }
        // }

        // TODO observe?
        // while (session.syncState().value !is SyncState.Idle) {
        //     sleep(100)
        // }

        session.open()
        session.startSync(true)
        //await(lock)
        //session.syncState().removeObserver(observer)
    }

//     /**
//      * Sends text messages in a room
//      *
//      * @param room         the room where to send the messages
//      * @param message      the message to send
//      * @param nbOfMessages the number of time the message will be sent
//      * @throws Exception
//      */
//     @Throws(Exception::class)
//     fun sendTextMessage(room: Room, message: String, nbOfMessages: Int): List<Event> {
//         val sentEvents = ArrayList<Event>(nbOfMessages)
//         val latch = CountDownLatch(nbOfMessages)
//         val onEventSentListener = object : MXEventListener() {
//             fun onEventSent(event: Event, prevEventId: String) {
//                 latch.countDown()
//             }
//         }
//         room.addEventListener(onEventSentListener)
//         for (i in 0 until nbOfMessages) {
//             room.sendTextMessage(message + " #" + (i + 1), null, Message.FORMAT_MATRIX_HTML, object : RoomMediaMessage.EventCreationListener() {
//                 fun onEventCreated(roomMediaMessage: RoomMediaMessage) {
//                     val sentEvent = roomMediaMessage.getEvent()
//                     sentEvents.add(sentEvent)
//                 }
//
//                 fun onEventCreationFailed(roomMediaMessage: RoomMediaMessage, errorMessage: String) {
//
//                 }
//
//                 fun onEncryptionFailed(roomMediaMessage: RoomMediaMessage) {
//
//                 }
//             })
//         }
//         await(latch)
//         room.removeEventListener(onEventSentListener)
//
//         // Check that all events has been created
//         Assert.assertEquals(nbOfMessages.toLong(), sentEvents.size.toLong())
//
//         return sentEvents
//     }
//
//
    // PRIVATE METHODS *****************************************************************************

    /**
     * Creates a unique account
     *
     * @param userNamePrefix the user name prefix
     * @param password       the password
     * @param testParams     test params about the session
     * @return the session associated with the newly created account
     */
    @Throws(InterruptedException::class)
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
    @Throws(InterruptedException::class)
    private fun logIntoAccount(userId: String,
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

        var lock = CountDownLatch(1)
        matrix.authenticationService.getLoginFlow(hs, object : TestMatrixCallback<LoginFlowResult>(lock) {})
        await(lock)

        lock = CountDownLatch(1)
        matrix.authenticationService.getRegistrationWizard().createAccount(userName, password, null, object : TestMatrixCallback<RegistrationResult>(lock) {
            override fun onSuccess(data: RegistrationResult) {
                super.onSuccess(data)
            }
        })
        await(lock)

        // Preform dummy step
        lock = CountDownLatch(1)
        var registrationResult: RegistrationResult? = null
        matrix.authenticationService.getRegistrationWizard().dummy(object : TestMatrixCallback<RegistrationResult>(lock) {
            override fun onSuccess(data: RegistrationResult) {
                registrationResult = data
                super.onSuccess(data)
            }
        })
        await(lock)

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

        var lock = CountDownLatch(1)
        matrix.authenticationService.getLoginFlow(hs, object : TestMatrixCallback<LoginFlowResult>(lock) {})
        await(lock)

        lock = CountDownLatch(1)
        var session: Session? = null
        matrix.authenticationService.getLoginWizard().login(userName, password, "myDevice", object : TestMatrixCallback<Session>(lock) {
            override fun onSuccess(data: Session) {
                session = data
                super.onSuccess(data)
            }
        })
        await(lock)

        assertNotNull(session)

        if (sessionTestParams.withInitialSync) {
            syncSession(session!!)
        }

        return session!!
    }

    /**
     * Await for a latch and ensure the result is true
     *
     * @param latch
     * @throws InterruptedException
     */
    fun await(latch: CountDownLatch) {
        assertTrue(latch.await(TestConstants.timeOutMillis, TimeUnit.MILLISECONDS))
    }

    /**
     * Clear all provided sessions
     *
     * @param sessions the sessions to clear
     */
    fun closeAllSessions(sessions: List<Session>) {
        for (session in sessions) {
            session.close()
        }
    }

    fun signout(session: Session) {
        val lock = CountDownLatch(1)
        session.signOut(true, object : TestMatrixCallback<Unit>(lock) {})
        await(lock)
    }


//     /**
//      * Clone a session.
//      * It simulate that the user launches again the application with the same Credentials, contrary to login which will create a new DeviceId
//      *
//      * @param from the session to clone
//      * @return the duplicated session
//      */
//     @Throws(InterruptedException::class)
//     fun createNewSession(from: Session, sessionTestParams: SessionTestParams): Session {
//         val context = InstrumentationRegistry.getContext()
//
//         val credentials = from.sessionParams.credentials
//         val hs = createHomeServerConfig(credentials)
//         val store = MXFileStore(hs, false, context)
//         val dataHandler = MXDataHandler(store, credentials)
//         dataHandler.setLazyLoadingEnabled(sessionTestParams.withLazyLoading)
//         store.setDataHandler(dataHandler)
//         val session2 = Session.Builder(hs, dataHandler, context)
//                 .withLegacyCryptoStore(sessionTestParams.withLegacyCryptoStore)
//                 .build()
//
//         val results = HashMap<String, Any>()
//
//         val lock = CountDownLatch(1)
//         val listener = object : MXStoreListener() {
//             fun postProcess(accountId: String) {
//                 results["postProcess"] = "postProcess $accountId"
//             }
//
//             fun onStoreReady(accountId: String) {
//                 results["onStoreReady"] = "onStoreReady"
//                 lock.countDown()
//             }
//
//             fun onStoreCorrupted(accountId: String, description: String) {
//                 results["onStoreCorrupted"] = description
//                 lock.countDown()
//             }
//
//             fun onStoreOOM(accountId: String, description: String) {
//                 results["onStoreOOM"] = "onStoreOOM"
//                 lock.countDown()
//             }
//         }
//
//         store.addMXStoreListener(listener)
//         store.open()
//
//         await(lock)
//
//         Assert.assertTrue(results.toString(), results.containsKey("onStoreReady"))
//
//         return session2
//     }
}
