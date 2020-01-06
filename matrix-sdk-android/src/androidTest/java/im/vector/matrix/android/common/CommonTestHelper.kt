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

// import android.content.Context
// import android.net.Uri
// import androidx.test.InstrumentationRegistry
//
// import org.junit.Assert
//
// import java.util.ArrayList
// import java.util.HashMap
// import java.util.UUID
// import java.util.concurrent.CountDownLatch
// import java.util.concurrent.TimeUnit
//
// import im.vector.matrix.android.api.Matrix
// import im.vector.matrix.android.api.auth.data.Credentials
// import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
// import im.vector.matrix.android.api.session.Session
// import im.vector.matrix.android.internal.auth.registration.AuthParams
// import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
// import im.vector.matrix.android.internal.auth.registration.RegistrationParams
// import io.realm.internal.android.JsonUtils
//
//
// /**
//  * This class exposes methods to be used in common cases
//  * Registration, login, Sync, Sending messages...
//  */
// class CommonTestHelper {
//
//     @Throws(InterruptedException::class)
//     fun createAccount(userNamePrefix: String, testParams: SessionTestParams): Session {
//         return createAccount(userNamePrefix, TestConstants.PASSWORD, testParams)
//     }
//
//     @Throws(InterruptedException::class)
//     fun logIntoAccount(userId: String, testParams: SessionTestParams): Session {
//         return logIntoAccount(userId, TestConstants.PASSWORD, testParams)
//     }
//
//     /**
//      * Create a Home server configuration, with Http connection allowed for test
//      */
//     fun createHomeServerConfig(): HomeServerConnectionConfig {
//         return HomeServerConnectionConfig.Builder()
//                 .withHomeServerUri(Uri.parse(TestConstants.TESTS_HOME_SERVER_URL))
//                 .build()
//     }
//
//     /**
//      * This methods init the event stream and check for initial sync
//      *
//      * @param session    the session to sync
//      * @param withCrypto true if crypto is enabled and should be checked
//      */
//     @Throws(InterruptedException::class)
//     fun syncSession(session: Session, withCrypto: Boolean) {
//         val params = HashMap<String, Boolean>()
//         val sizeOfLock = if (withCrypto) 2 else 1
//         val lock2 = CountDownLatch(sizeOfLock)
//         session.getDataHandler().addListener(object : MXEventListener() {
//             fun onInitialSyncComplete(toToken: String) {
//                 params["isInit"] = true
//                 lock2.countDown()
//             }
//
//             fun onCryptoSyncComplete() {
//                 params["onCryptoSyncComplete"] = true
//                 lock2.countDown()
//             }
//         })
//         session.getDataHandler().getStore().open()
//         session.startEventStream(null)
//
//         await(lock2)
//         Assert.assertTrue(params.containsKey("isInit"))
//         if (withCrypto) {
//             Assert.assertTrue(params.containsKey("onCryptoSyncComplete"))
//         }
//     }
//
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
//     // PRIVATE METHODS *****************************************************************************
//
//     /**
//      * Creates a unique account
//      *
//      * @param userNamePrefix the user name prefix
//      * @param password       the password
//      * @param testParams     test params about the session
//      * @return the session associated with the newly created account
//      */
//     @Throws(InterruptedException::class)
//     private fun createAccount(userNamePrefix: String,
//                               password: String,
//                               testParams: SessionTestParams): Session {
//         val context = InstrumentationRegistry.getContext()
//         val session = createAccountAndSync(
//                 context,
//                 userNamePrefix + "_" + System.currentTimeMillis() + UUID.randomUUID(),
//                 password,
//                 testParams
//         )
//         Assert.assertNotNull(session)
//         return session
//     }
//
//     /**
//      * Logs into an existing account
//      *
//      * @param userId     the userId to log in
//      * @param password   the password to log in
//      * @param testParams test params about the session
//      * @return the session associated with the existing account
//      */
//     @Throws(InterruptedException::class)
//     private fun logIntoAccount(userId: String,
//                                password: String,
//                                testParams: SessionTestParams): Session {
//         val context = InstrumentationRegistry.getContext()
//         val session = logAccountAndSync(context, userId, password, testParams)
//         Assert.assertNotNull(session)
//         return session
//     }
//
//     /**
//      * Create an account and a dedicated session
//      *
//      * @param context           the context
//      * @param userName          the account username
//      * @param password          the password
//      * @param sessionTestParams parameters for the test
//      */
//     @Throws(InterruptedException::class)
//     private fun createAccountAndSync(context: Context,
//                                      userName: String,
//                                      password: String,
//                                      sessionTestParams: SessionTestParams): Session {
//         val hs = createHomeServerConfig()
//
//         val loginRestClient = LoginRestClient(hs)
//
//         val params = HashMap<String, Any>()
//         val registrationParams = RegistrationParams()
//
//         var lock = CountDownLatch(1)
//
//         // get the registration session id
//         loginRestClient.register(registrationParams, object : TestMatrixCallback<Credentials>(lock, false) {
//             override fun onFailure(failure: Throwable) {
//                 // detect if a parameter is expected
//                 var registrationFlowResponse: RegistrationFlowResponse? = null
//
//                 // when a response is not completed the server returns an error message
//                 if (null != failure.mStatus && failure.mStatus === 401) {
//                     try {
//                         registrationFlowResponse = JsonUtils.toRegistrationFlowResponse(e.mErrorBodyAsString)
//                     } catch (castExcept: Exception) {
//                     }
//
//                 }
//
//                 // check if the server response can be casted
//                 if (null != registrationFlowResponse) {
//                     params["session"] = registrationFlowResponse!!.session
//                 }
//
//                 super.onFailure(failure)
//             }
//         })
//
//         await(lock)
//
//         val session = params["session"] as String?
//
//         Assert.assertNotNull(session)
//
//         registrationParams.username = userName
//         registrationParams.password = password
//         val authParams = AuthParams(LOGIN_FLOW_TYPE_DUMMY)
//         authParams.session = session
//
//         registrationParams.auth = authParams
//
//         lock = CountDownLatch(1)
//         loginRestClient.register(registrationParams, object : TestMatrixCallback<Credentials>(lock) {
//             fun onSuccess(credentials: Credentials) {
//                 params["credentials"] = credentials
//                 super.onSuccess(credentials)
//             }
//         })
//
//         await(lock)
//
//         val credentials = params["credentials"] as Credentials?
//
//         Assert.assertNotNull(credentials)
//
//         hs.setCredentials(credentials)
//
//         val store = MXFileStore(hs, false, context)
//
//         val dataHandler = MXDataHandler(store, credentials)
//         dataHandler.setLazyLoadingEnabled(sessionTestParams.withLazyLoading)
//
//         val Session = Session.Builder(hs, dataHandler, context)
//                 .withLegacyCryptoStore(sessionTestParams.withLegacyCryptoStore)
//                 .build()
//
//         if (sessionTestParams.withCryptoEnabled) {
//             Session.enableCryptoWhenStarting()
//         }
//         if (sessionTestParams.withInitialSync) {
//             syncSession(Session, sessionTestParams.withCryptoEnabled)
//         }
//         return Session
//     }
//
//     /**
//      * Start an account login
//      *
//      * @param context           the context
//      * @param userName          the account username
//      * @param password          the password
//      * @param sessionTestParams session test params
//      */
//     @Throws(InterruptedException::class)
//     private fun logAccountAndSync(context: Context,
//                                   userName: String,
//                                   password: String,
//                                   sessionTestParams: SessionTestParams): Session {
//         val hs = createHomeServerConfig(null)
//         val loginRestClient = LoginRestClient(hs)
//         val params = HashMap<String, Any>()
//         val lock = CountDownLatch(1)
//
//         // get the registration session id
//         loginRestClient.loginWithUser(userName, password, object : TestMatrixCallback<Credentials>(lock) {
//             fun onSuccess(credentials: Credentials) {
//                 params["credentials"] = credentials
//                 super.onSuccess(credentials)
//             }
//         })
//
//         await(lock)
//
//         val credentials = params["credentials"] as Credentials?
//
//         Assert.assertNotNull(credentials)
//
//         hs.setCredentials(credentials)
//
//         val store = MXFileStore(hs, false, context)
//
//         val mxDataHandler = MXDataHandler(store, credentials)
//         mxDataHandler.setLazyLoadingEnabled(sessionTestParams.withLazyLoading)
//
//         val Session = Session.Builder(hs, mxDataHandler, context)
//                 .withLegacyCryptoStore(sessionTestParams.withLegacyCryptoStore)
//                 .build()
//
//         if (sessionTestParams.withCryptoEnabled) {
//             Session.enableCryptoWhenStarting()
//         }
//         if (sessionTestParams.withInitialSync) {
//             syncSession(Session, sessionTestParams.withCryptoEnabled)
//         }
//         return Session
//     }
//
//     /**
//      * Await for a latch and ensure the result is true
//      *
//      * @param latch
//      * @throws InterruptedException
//      */
//     @Throws(InterruptedException::class)
//     fun await(latch: CountDownLatch) {
//         Assert.assertTrue(latch.await(TestConstants.timeOutMillis, TimeUnit.MILLISECONDS))
//     }
//
//     /**
//      * Clear all provided sessions
//      *
//      * @param sessions the sessions to clear
//      */
//     fun closeAllSessions(sessions: List<Session>) {
//         for (session in sessions) {
//             session.close()
//         }
//     }
//
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
// }
