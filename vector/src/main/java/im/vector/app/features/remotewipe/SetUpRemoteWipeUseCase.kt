/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.remotewipe

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.session.events.model.toContent
import timber.log.Timber
import javax.inject.Inject

/**
 * Check the user's account data for this device.
 * If a remote wipe nonce has been registered, do nothing.
 * If it has not been registered, register a new nonce.
 */
class SetUpRemoteWipeUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val generateRemoteWipeNonceUseCase: GenerateRemoteWipeNonceUseCase
) {
    suspend fun execute() {
        if(!activeSessionHolder.hasActiveSession()) {
            Timber.d("## Remote wipe: cancelling as there is no active session")
            return
        }

        val session = activeSessionHolder.getActiveSession()

        val isBackendEnabled = session.synapseService()
                .isFeatureEnabled(RemoteWipeSynapseFeature.FEATURE_ID)

        if(!isBackendEnabled) {
            Timber.d("## Remote wipe: not supported by homeserver")
            return
        }

        val deviceId = session.sessionParams.deviceId

        if (deviceId == null) {
            Timber.e(RemoteWipeException("Could not set up remote wipe without valid device ID"))
            return
        }

        val accountDataService = session.accountDataService()
        val accountDataKey = RemoteWipeAccountData.accountDataKey(deviceId)

        val accountData = accountDataService
                .getUserAccountDataEvents(setOf(accountDataKey))
                .firstOrNull()

        if (accountData != null) {
            Timber.d("## Remote wipe: nonce is already set ${accountData.content}")
            return
        }

        val newAccountData = mapOf(
                "nonce" to generateRemoteWipeNonceUseCase.generateNonce()
        )

        accountDataService.updateUserAccountData(
                accountDataKey,
                newAccountData.toContent()
        )

        Timber.d("## Remote wipe: created new nonce: ${newAccountData["nonce"]}")
    }

}
