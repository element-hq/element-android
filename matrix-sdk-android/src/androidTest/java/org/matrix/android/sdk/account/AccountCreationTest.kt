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

package org.matrix.android.sdk.account

import android.util.Log
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class AccountCreationTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    @Test
    fun createAccountTest() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        commonTestHelper.signOutAndClose(session)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun createAccountAndLoginAgainTest() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        // Log again to the same account
        val session2 = commonTestHelper.logIntoAccount(session.myUserId, SessionTestParams(withInitialSync = true))

        commonTestHelper.signOutAndClose(session)
        commonTestHelper.signOutAndClose(session2)
    }

    @Test
    fun simpleE2eTest() {
        val res = cryptoTestHelper.doE2ETestWithAliceInARoom()

        res.cleanUp(commonTestHelper)
    }

    @Test
    fun testConcurrentDecrypt() {
//        val res = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        // =============================
        // ARRANGE
        // =============================

        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val bobSession = commonTestHelper.createAccount(TestConstants.USER_BOB, SessionTestParams(true))
        cryptoTestHelper.initializeCrossSigning(bobSession)
        val bobSession2 = commonTestHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        bobSession2.cryptoService().verificationService().markedLocallyAsManuallyVerified(bobSession.myUserId, bobSession.sessionParams.deviceId ?: "")
        bobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(bobSession.myUserId, bobSession2.sessionParams.deviceId ?: "")

        val roomId = cryptoTestHelper.createDM(aliceSession, bobSession)
        val roomAlicePOV = aliceSession.getRoom(roomId)!!

        // =============================
        // ACT
        // =============================

        val timelineEvent = commonTestHelper.sendTextMessage(roomAlicePOV, "Hello Bob", 1).first()
        val secondEvent = commonTestHelper.sendTextMessage(roomAlicePOV, "Hello Bob 2", 1).first()
        val thirdEvent = commonTestHelper.sendTextMessage(roomAlicePOV, "Hello Bob 3", 1).first()
        val forthEvent = commonTestHelper.sendTextMessage(roomAlicePOV, "Hello Bob 4", 1).first()

        // await for bob unverified session to get the message
        commonTestHelper.waitWithLatch { latch ->
            commonTestHelper.retryPeriodicallyWithLatch(latch) {
                bobSession.getRoom(roomId)?.getTimeLineEvent(forthEvent.eventId) != null
            }
        }

        val eventBobPOV = bobSession.getRoom(roomId)?.getTimeLineEvent(timelineEvent.eventId)!!
        val secondEventBobPOV = bobSession.getRoom(roomId)?.getTimeLineEvent(secondEvent.eventId)!!
        val thirdEventBobPOV = bobSession.getRoom(roomId)?.getTimeLineEvent(thirdEvent.eventId)!!
        val forthEventBobPOV = bobSession.getRoom(roomId)?.getTimeLineEvent(forthEvent.eventId)!!

        // let's try to decrypt concurrently and check that we are not getting exceptions
        val dispatcher = Executors
                .newFixedThreadPool(100)
                .asCoroutineDispatcher()
        val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

        val eventList = listOf(eventBobPOV, secondEventBobPOV, thirdEventBobPOV, forthEventBobPOV)

//        commonTestHelper.runBlockingTest {
//            val export = bobSession.cryptoService().exportRoomKeys("foo")

//        }
        val atomicAsError = AtomicBoolean()
        val deff = mutableListOf<Deferred<Any>>()
//        for (i in 1..100) {
//            GlobalScope.launch {
//                val index = Random.nextInt(eventList.size)
//                try {
//                    val event = eventList[index]
//                    bobSession.cryptoService().decryptEvent(event.root, "")
//                    Log.d("#TEST", "Decrypt Success $index :${Thread.currentThread().name}")
//                } catch (failure: Throwable) {
//                    Log.d("#TEST", "Failed to decrypt $index  :$failure")
//                }
//            }
//        }
        val cryptoService = bobSession.cryptoService()

        coroutineScope.launch {
            for (spawn in 1..100) {
                delay((Random.nextFloat() * 1000).toLong())
                aliceSession.cryptoService().requestRoomKeyForEvent(eventList.random().root)
            }
        }

        for (spawn in 1..8000) {
            eventList.random().let { event ->
                coroutineScope.async {
                    try {
                        cryptoService.decryptEvent(event.root, "")
                        Log.d("#TEST", "[$spawn] Decrypt Success ${event.eventId} :${Thread.currentThread().name}")
                    } catch (failure: Throwable) {
                        atomicAsError.set(true)
                        Log.e("#TEST", "Failed to decrypt $spawn/${event.eventId}  :$failure")
                    }
                }.let {
                    deff.add(it)
                }
            }
//            coroutineScope.async {
//                val index = Random.nextInt(eventList.size)
//                try {
//                    val event = eventList[index]
//                    cryptoService.decryptEvent(event.root, "")
//                    for (other in eventList.indices) {
//                        if (other != index) {
//                            cryptoService.decryptEventAsync(eventList[other].root, "", object : MatrixCallback<MXEventDecryptionResult> {
//                                override fun onFailure(failure: Throwable) {
//                                    Log.e("#TEST", "Failed to decrypt $spawn/$index  :$failure")
//                                }
//                            })
//                        }
//                    }
//                    Log.d("#TEST", "[$spawn] Decrypt Success $index :${Thread.currentThread().name}")
//                } catch (failure: Throwable) {
//                    Log.e("#TEST", "Failed to decrypt $spawn/$index  :$failure")
//                }
//            }.let {
//                deff.add(it)
//            }
        }

        coroutineScope.launch {
            for (spawn in 1..100) {
                delay((Random.nextFloat() * 1000).toLong())
                bobSession.cryptoService().requestRoomKeyForEvent(eventList.random().root)
            }
        }

        commonTestHelper.runBlockingTest(10 * 60_000) {
            deff.awaitAll()
            delay(10_000)
            assert(!atomicAsError.get())
            // There should be no errors?
//            deff.map { it.await() }.forEach {
//                it.fold({
//                    Log.d("#TEST", "Decrypt Success :${it}")
//                }, {
//                    Log.d("#TEST", "Failed to decrypt :$it")
//                })
//                val hasFailure = deff.any { it.await().exceptionOrNull() != null }
//                assert(!hasFailure)
//            }

            commonTestHelper.signOutAndClose(aliceSession)
            commonTestHelper.signOutAndClose(bobSession)
            commonTestHelper.signOutAndClose(bobSession2)
        }
    }
}
