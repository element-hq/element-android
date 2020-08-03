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

package im.vector.matrix.android.internal.session.terms

import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.terms.GetTermsResponse
import im.vector.matrix.android.api.session.terms.TermsService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.di.UnauthenticatedWithCertificate
import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.identity.IdentityAuthAPI
import im.vector.matrix.android.internal.session.identity.IdentityRegisterTask
import im.vector.matrix.android.internal.session.openid.GetOpenIdTokenTask
import im.vector.matrix.android.internal.session.sync.model.accountdata.AcceptedTermsContent
import im.vector.matrix.android.api.session.accountdata.UserAccountDataTypes
import im.vector.matrix.android.internal.session.user.accountdata.AccountDataDataSource
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.launchToCallback
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.ensureTrailingSlash
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class DefaultTermsService @Inject constructor(
        @UnauthenticatedWithCertificate
        private val unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
        private val accountDataDataSource: AccountDataDataSource,
        private val termsAPI: TermsAPI,
        private val retrofitFactory: RetrofitFactory,
        private val getOpenIdTokenTask: GetOpenIdTokenTask,
        private val identityRegisterTask: IdentityRegisterTask,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor
) : TermsService {
    override fun getTerms(serviceType: TermsService.ServiceType,
                          baseUrl: String,
                          callback: MatrixCallback<GetTermsResponse>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            val url = buildUrl(baseUrl, serviceType)
            val termsResponse = executeRequest<TermsResponse>(null) {
                apiCall = termsAPI.getTerms("${url}terms")
            }
            GetTermsResponse(termsResponse, getAlreadyAcceptedTermUrlsFromAccountData())
        }
    }

    override fun agreeToTerms(serviceType: TermsService.ServiceType,
                              baseUrl: String,
                              agreedUrls: List<String>,
                              token: String?,
                              callback: MatrixCallback<Unit>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            val url = buildUrl(baseUrl, serviceType)
            val tokenToUse = token?.takeIf { it.isNotEmpty() } ?: getToken(baseUrl)

            executeRequest<Unit>(null) {
                apiCall = termsAPI.agreeToTerms("${url}terms", AcceptTermsBody(agreedUrls), "Bearer $tokenToUse")
            }

            // client SHOULD update this account data section adding any the URLs
            // of any additional documents that the user agreed to this list.
            // Get current m.accepted_terms append new ones and update account data
            val listOfAcceptedTerms = getAlreadyAcceptedTermUrlsFromAccountData()

            val newList = listOfAcceptedTerms.toMutableSet().apply { addAll(agreedUrls) }.toList()

            updateUserAccountDataTask.execute(UpdateUserAccountDataTask.AcceptedTermsParams(
                    acceptedTermsContent = AcceptedTermsContent(newList)
            ))
        }
    }

    private suspend fun getToken(url: String): String {
        // TODO This is duplicated code see DefaultIdentityService
        val api = retrofitFactory.create(unauthenticatedOkHttpClient, url).create(IdentityAuthAPI::class.java)

        val openIdToken = getOpenIdTokenTask.execute(Unit)
        val token = identityRegisterTask.execute(IdentityRegisterTask.Params(api, openIdToken))

        return token.token
    }

    private fun buildUrl(baseUrl: String, serviceType: TermsService.ServiceType): String {
        val servicePath = when (serviceType) {
            TermsService.ServiceType.IntegrationManager -> NetworkConstants.URI_INTEGRATION_MANAGER_PATH
            TermsService.ServiceType.IdentityService    -> NetworkConstants.URI_IDENTITY_PATH_V2
        }
        return "${baseUrl.ensureTrailingSlash()}$servicePath"
    }

    private fun getAlreadyAcceptedTermUrlsFromAccountData(): Set<String> {
        return accountDataDataSource.getAccountDataEvent(UserAccountDataTypes.TYPE_ACCEPTED_TERMS)
                ?.content
                ?.toModel<AcceptedTermsContent>()
                ?.acceptedTerms
                ?.toSet()
                .orEmpty()
    }
}
