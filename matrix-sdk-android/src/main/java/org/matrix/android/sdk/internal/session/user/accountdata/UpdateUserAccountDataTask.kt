/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.integrationmanager.AllowedWidgetsContent
import org.matrix.android.sdk.internal.session.integrationmanager.IntegrationProvisioningContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.AcceptedTermsContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.BreadcrumbsContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.IdentityServerContent
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UpdateUserAccountDataTask : Task<UpdateUserAccountDataTask.Params, Unit> {

    interface Params {
        val type: String
        fun getData(): Any
    }

    data class IdentityParams(
            override val type: String = UserAccountDataTypes.TYPE_IDENTITY_SERVER,
            private val identityContent: IdentityServerContent
    ) : Params {

        override fun getData(): Any {
            return identityContent
        }
    }

    data class AcceptedTermsParams(
            override val type: String = UserAccountDataTypes.TYPE_ACCEPTED_TERMS,
            private val acceptedTermsContent: AcceptedTermsContent
    ) : Params {

        override fun getData(): Any {
            return acceptedTermsContent
        }
    }

    // TODO Use [UserAccountDataDirectMessages] class?
    data class DirectChatParams(
            override val type: String = UserAccountDataTypes.TYPE_DIRECT_MESSAGES,
            private val directMessages: Map<String, List<String>>
    ) : Params {

        override fun getData(): Any {
            return directMessages
        }
    }

    data class BreadcrumbsParams(
            override val type: String = UserAccountDataTypes.TYPE_BREADCRUMBS,
            private val breadcrumbsContent: BreadcrumbsContent
    ) : Params {

        override fun getData(): Any {
            return breadcrumbsContent
        }
    }

    data class AllowedWidgets(
            override val type: String = UserAccountDataTypes.TYPE_ALLOWED_WIDGETS,
            private val allowedWidgetsContent: AllowedWidgetsContent
    ) : Params {

        override fun getData(): Any {
            return allowedWidgetsContent
        }
    }

    data class IntegrationProvisioning(
            override val type: String = UserAccountDataTypes.TYPE_INTEGRATION_PROVISIONING,
            private val integrationProvisioningContent: IntegrationProvisioningContent
    ) : Params {

        override fun getData(): Any {
            return integrationProvisioningContent
        }
    }

    data class AnyParams(
            override val type: String,
            private val any: Any
    ) : Params {
        override fun getData(): Any {
            return any
        }
    }
}

internal class DefaultUpdateUserAccountDataTask @Inject constructor(
        private val accountDataApi: AccountDataAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UpdateUserAccountDataTask {

    override suspend fun execute(params: UpdateUserAccountDataTask.Params) {
        return executeRequest(globalErrorReceiver) {
            accountDataApi.setAccountData(userId, params.type, params.getData())
        }
    }
}
