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

package org.matrix.android.sdk.internal.session.pushers

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.pushrules.ConditionResolver
import org.matrix.android.sdk.api.pushrules.PushRuleService
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.internal.session.notification.DefaultProcessEventForPushTask
import org.matrix.android.sdk.internal.session.notification.DefaultPushRuleService
import org.matrix.android.sdk.internal.session.notification.ProcessEventForPushTask
import org.matrix.android.sdk.internal.session.pushers.gateway.DefaultPushGatewayNotifyTask
import org.matrix.android.sdk.internal.session.pushers.gateway.PushGatewayNotifyTask
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
