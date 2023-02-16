/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.rendezvous

import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.with
import org.junit.Test
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.rendezvous.channels.ECDHRendezvousChannel
import org.matrix.android.sdk.api.rendezvous.model.RendezvousError
import org.matrix.android.sdk.common.CommonTestHelper

class RendezvousTest : InstrumentedTest {

    @Test
    fun shouldSuccessfullyBuildChannels() = CommonTestHelper.runCryptoTest(context()) { _, _ ->
        val cases = listOf(
            // v1:
            "{\"rendezvous\":{\"algorithm\":\"org.matrix.msc3903.rendezvous.v1.curve25519-aes-sha256\"," +
            "\"key\":\"aeSGwYTV1IUhikUyCapzC6p2xG5NpJ4Lwj2UgUMlcTk\",\"transport\":" +
            "{\"type\":\"org.matrix.msc3886.http.v1\",\"uri\":\"https://rendezvous.lab.element.dev/bcab62cd-3e34-48b4-bc39-90895da8f6fe\"}}," +
            "\"intent\":\"login.reciprocate\"}",
            // v2:
            "{\"rendezvous\":{\"algorithm\":\"org.matrix.msc3903.rendezvous.v2.curve25519-aes-sha256\"," +
            "\"key\":\"aeSGwYTV1IUhikUyCapzC6p2xG5NpJ4Lwj2UgUMlcTk\",\"transport\":" +
            "{\"type\":\"org.matrix.msc3886.http.v1\",\"uri\":\"https://rendezvous.lab.element.dev/bcab62cd-3e34-48b4-bc39-90895da8f6fe\"}}," +
            "\"intent\":\"login.reciprocate\"}",
        )

        cases.forEach { input ->
            Rendezvous.buildChannelFromCode(input).channel shouldBeInstanceOf ECDHRendezvousChannel::class
        }
    }

    @Test
    fun shouldFailToBuildChannelAsUnsupportedAlgorithm() {
        invoking {
            Rendezvous.buildChannelFromCode(
                "{\"rendezvous\":{\"algorithm\":\"bad algo\"," +
                "\"key\":\"aeSGwYTV1IUhikUyCapzC6p2xG5NpJ4Lwj2UgUMlcTk\",\"transport\":" +
                "{\"type\":\"org.matrix.msc3886.http.v1\",\"uri\":\"https://rendezvous.lab.element.dev/bcab62cd-3e34-48b4-bc39-90895da8f6fe\"}}," +
                "\"intent\":\"login.reciprocate\"}"
            )
        } shouldThrow RendezvousError::class with {
            this.reason shouldBeEqualTo RendezvousFailureReason.UnsupportedAlgorithm
        }
    }

    @Test
    fun shouldFailToBuildChannelAsUnsupportedTransport() {
        invoking {
            Rendezvous.buildChannelFromCode(
                    "{\"rendezvous\":{\"algorithm\":\"org.matrix.msc3903.rendezvous.v1.curve25519-aes-sha256\"," +
                            "\"key\":\"aeSGwYTV1IUhikUyCapzC6p2xG5NpJ4Lwj2UgUMlcTk\",\"transport\":" +
                            "{\"type\":\"bad transport\",\"uri\":\"https://rendezvous.lab.element.dev/bcab62cd-3e34-48b4-bc39-90895da8f6fe\"}}," +
                            "\"intent\":\"login.reciprocate\"}"
            )
        } shouldThrow RendezvousError::class with {
            this.reason shouldBeEqualTo RendezvousFailureReason.UnsupportedTransport
        }
    }

    @Test
    fun shouldFailToBuildChannelWithInvalidIntent() {
        invoking {
            Rendezvous.buildChannelFromCode(
                    "{\"rendezvous\":{\"algorithm\":\"org.matrix.msc3903.rendezvous.v1.curve25519-aes-sha256\"," +
                            "\"key\":\"aeSGwYTV1IUhikUyCapzC6p2xG5NpJ4Lwj2UgUMlcTk\",\"transport\":" +
                            "{\"type\":\"org.matrix.msc3886.http.v1\",\"uri\":\"https://rendezvous.lab.element.dev/bcab62cd-3e34-48b4-bc39-90895da8f6fe\"}}," +
                            "\"intent\":\"foo\"}"
            )
        } shouldThrow RendezvousError::class with {
            this.reason shouldBeEqualTo RendezvousFailureReason.InvalidCode
        }
    }

    @Test
    fun shouldFailToBuildChannelAsInvalidCode() {
        val cases = listOf(
            "{}",
            "rubbish",
            ""
        )

        cases.forEach { input ->
            invoking {
                Rendezvous.buildChannelFromCode(input)
            } shouldThrow RendezvousError::class with {
                this.reason shouldBeEqualTo RendezvousFailureReason.InvalidCode
            }
        }
    }
}
