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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.util.Optional

internal data class UserIdentityCollector(val userId: String, val collector: SendChannel<Optional<MXCrossSigningInfo>>) :
        SendChannel<Optional<MXCrossSigningInfo>> by collector

internal data class DevicesCollector(val userIds: List<String>, val collector: SendChannel<List<CryptoDeviceInfo>>) :
        SendChannel<List<CryptoDeviceInfo>> by collector

private typealias PrivateKeysCollector = SendChannel<Optional<PrivateKeysInfo>>

internal class FlowCollectors {
    private val userIdentityCollectors = mutableListOf<UserIdentityCollector>()
    private val privateKeyCollectors = mutableListOf<PrivateKeysCollector>()
    private val deviceCollectors = ArrayList<DevicesCollector>()

    private val identityLock = Mutex()
    private val keysLock = Mutex()
    private val deviceLock = Mutex()

    suspend fun addIdentityCollector(collector: UserIdentityCollector) {
        identityLock.withLock {
            userIdentityCollectors.add(collector)
        }
    }

    fun removeIdentityCollector(collector: UserIdentityCollector) {
        // Annoying but it's called when the channel is closed and can't call
        // something suspendable there :/
        runBlocking {
            identityLock.withLock {
                userIdentityCollectors.remove(collector)
            }
        }
    }

    suspend fun forEachIdentityCollector(block: suspend ((UserIdentityCollector) -> Unit)) {
        val safeCopy = identityLock.withLock {
            userIdentityCollectors.toList()
        }
        safeCopy.onEach { block(it) }
    }

    suspend fun addPrivateKeysCollector(collector: PrivateKeysCollector) {
        keysLock.withLock {
            privateKeyCollectors.add(collector)
        }
    }

    fun removePrivateKeysCollector(collector: PrivateKeysCollector) {
        // Annoying but it's called when the channel is closed and can't call
        // something suspendable there :/
        runBlocking {
            keysLock.withLock {
                privateKeyCollectors.remove(collector)
            }
        }
    }

    suspend fun forEachPrivateKeysCollector(block: suspend ((PrivateKeysCollector) -> Unit)) {
        val safeCopy = keysLock.withLock {
            privateKeyCollectors.toList()
        }
        safeCopy.onEach { block(it) }
    }

    suspend fun addDevicesCollector(collector: DevicesCollector) {
        deviceLock.withLock {
            deviceCollectors.add(collector)
        }
    }

    fun removeDevicesCollector(collector: DevicesCollector) {
        // Annoying but it's called when the channel is closed and can't call
        // something suspendable there :/
        runBlocking {
            deviceLock.withLock {
                deviceCollectors.remove(collector)
            }
        }
    }

    suspend fun forEachDevicesCollector(block: suspend ((DevicesCollector) -> Unit)) {
        val safeCopy = deviceLock.withLock {
            deviceCollectors.toList()
        }
        safeCopy.onEach { block(it) }
    }
}
