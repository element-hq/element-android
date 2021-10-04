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

package org.matrix.android.sdk.internal.session.profile

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import java.util.UUID
import javax.inject.Inject

internal abstract class AddThreePidTask : Task<AddThreePidTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultAddThreePidTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val pendingThreePidMapper: PendingThreePidMapper,
        private val globalErrorReceiver: GlobalErrorReceiver) : AddThreePidTask() {

    override suspend fun execute(params: Params) {
        when (params.threePid) {
            is ThreePid.Email  -> addEmail(params.threePid)
            is ThreePid.Msisdn -> addMsisdn(params.threePid)
        }
    }

    private suspend fun addEmail(threePid: ThreePid.Email) {
        val clientSecret = UUID.randomUUID().toString()
        val sendAttempt = 1

        val body = AddEmailBody(
                clientSecret = clientSecret,
                email = threePid.email,
                sendAttempt = sendAttempt
        )

        val result = executeRequest(globalErrorReceiver) {
            profileAPI.addEmail(body)
        }

        // Store as a pending three pid
        monarchy.awaitTransaction { realm ->
            PendingThreePid(
                    threePid = threePid,
                    clientSecret = clientSecret,
                    sendAttempt = sendAttempt,
                    sid = result.sid,
                    submitUrl = null
            )
                    .let { pendingThreePidMapper.map(it) }
                    .let { realm.copyToRealm(it) }
        }
    }

    private suspend fun addMsisdn(threePid: ThreePid.Msisdn) {
        val clientSecret = UUID.randomUUID().toString()
        val sendAttempt = 1

        // Get country code and national number from the phone number
        val phoneNumber = threePid.msisdn
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        val parsedNumber = phoneNumberUtil.parse(phoneNumber, null)
        val countryCode = parsedNumber.countryCode
        val country = phoneNumberUtil.getRegionCodeForCountryCode(countryCode)

        val body = AddMsisdnBody(
                clientSecret = clientSecret,
                country = country,
                phoneNumber = parsedNumber.nationalNumber.toString(),
                sendAttempt = sendAttempt
        )

        val result = executeRequest(globalErrorReceiver) {
            profileAPI.addMsisdn(body)
        }

        // Store as a pending three pid
        monarchy.awaitTransaction { realm ->
            PendingThreePid(
                    threePid = threePid,
                    clientSecret = clientSecret,
                    sendAttempt = sendAttempt,
                    sid = result.sid,
                    submitUrl = result.submitUrl
            )
                    .let { pendingThreePidMapper.map(it) }
                    .let { realm.copyToRealm(it) }
        }
    }
}
