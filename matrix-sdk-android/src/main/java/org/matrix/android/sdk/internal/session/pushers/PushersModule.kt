/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.pushers

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.api.session.pushrules.ConditionResolver
import org.matrix.android.sdk.api.session.pushrules.PushRuleService
import org.matrix.android.sdk.internal.session.pushers.gateway.DefaultPushGatewayNotifyTask
import org.matrix.android.sdk.internal.session.pushers.gateway.PushGatewayNotifyTask
import org.matrix.android.sdk.internal.session.pushrules.DefaultProcessEventForPushTask
import org.matrix.android.sdk.internal.session.pushrules.DefaultPushRuleService
import org.matrix.android.sdk.internal.session.pushrules.ProcessEventForPushTask
import org.matrix.android.sdk.internal.session.room.notification.DefaultSetRoomNotificationStateTask
import org.matrix.android.sdk.internal.session.room.notification.SetRoomNotificationStateTask
import retrofit2.Retrofit

@Module
internal abstract class PushersModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun providesPushersAPI(retrofit: Retrofit): PushersAPI {
            return retrofit.create(PushersAPI::class.java)
        }

        @JvmStatic
        @Provides
        fun providesPushRulesApi(retrofit: Retrofit): PushRulesApi {
            return retrofit.create(PushRulesApi::class.java)
        }
    }

    @Binds
    abstract fun bindPusherService(service: DefaultPushersService): PushersService

    @Binds
    abstract fun bindConditionResolver(resolver: DefaultConditionResolver): ConditionResolver

    @Binds
    abstract fun bindGetPushersTask(task: DefaultGetPushersTask): GetPushersTask

    @Binds
    abstract fun bindGetPushRulesTask(task: DefaultGetPushRulesTask): GetPushRulesTask

    @Binds
    abstract fun bindSavePushRulesTask(task: DefaultSavePushRulesTask): SavePushRulesTask

    @Binds
    abstract fun bindAddPusherTask(task: DefaultAddPusherTask): AddPusherTask

    @Binds
    abstract fun bindRemovePusherTask(task: DefaultRemovePusherTask): RemovePusherTask

    @Binds
    abstract fun bindUpdatePushRuleEnableStatusTask(task: DefaultUpdatePushRuleEnableStatusTask): UpdatePushRuleEnableStatusTask

    @Binds
    abstract fun bindAddPushRuleTask(task: DefaultAddPushRuleTask): AddPushRuleTask

    @Binds
    abstract fun bindUpdatePushRuleActionTask(task: DefaultUpdatePushRuleActionsTask): UpdatePushRuleActionsTask

    @Binds
    abstract fun bindRemovePushRuleTask(task: DefaultRemovePushRuleTask): RemovePushRuleTask

    @Binds
    abstract fun bindSetRoomNotificationStateTask(task: DefaultSetRoomNotificationStateTask): SetRoomNotificationStateTask

    @Binds
    abstract fun bindPushRuleService(service: DefaultPushRuleService): PushRuleService

    @Binds
    abstract fun bindProcessEventForPushTask(task: DefaultProcessEventForPushTask): ProcessEventForPushTask

    @Binds
    abstract fun bindPushGatewayNotifyTask(task: DefaultPushGatewayNotifyTask): PushGatewayNotifyTask
}
