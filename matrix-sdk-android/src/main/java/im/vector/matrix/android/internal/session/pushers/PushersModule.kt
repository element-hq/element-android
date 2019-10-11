/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.pushers

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.pushrules.ConditionResolver
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.internal.session.notification.DefaultProcessEventForPushTask
import im.vector.matrix.android.internal.session.notification.DefaultPushRuleService
import im.vector.matrix.android.internal.session.notification.ProcessEventForPushTask
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
    abstract fun bindPusherService(pusherService: DefaultPusherService): PushersService

    @Binds
    abstract fun bindConditionResolver(conditionResolver: DefaultConditionResolver): ConditionResolver

    @Binds
    abstract fun bindGetPushersTask(getPushersTask: DefaultGetPushersTask): GetPushersTask

    @Binds
    abstract fun bindGetPushRulesTask(getPushRulesTask: DefaultGetPushRulesTask): GetPushRulesTask

    @Binds
    abstract fun bindSavePushRulesTask(savePushRulesTask: DefaultSavePushRulesTask): SavePushRulesTask

    @Binds
    abstract fun bindRemovePusherTask(removePusherTask: DefaultRemovePusherTask): RemovePusherTask

    @Binds
    abstract fun bindUpdatePushRuleEnableStatusTask(updatePushRuleEnableStatusTask: DefaultUpdatePushRuleEnableStatusTask): UpdatePushRuleEnableStatusTask

    @Binds
    abstract fun bindPushRuleService(pushRuleService: DefaultPushRuleService): PushRuleService

    @Binds
    abstract fun bindProcessEventForPushTask(processEventForPushTask: DefaultProcessEventForPushTask): ProcessEventForPushTask
}
