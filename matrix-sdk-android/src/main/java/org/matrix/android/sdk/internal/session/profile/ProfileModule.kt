/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.matrix.android.sdk.internal.session.profile

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.internal.session.SessionScope
import retrofit2.Retrofit

@Module
internal abstract class ProfileModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesProfileAPI(retrofit: Retrofit): ProfileAPI {
            return retrofit.create(ProfileAPI::class.java)
        }
    }

    @Binds
    abstract fun bindProfileService(service: DefaultProfileService): ProfileService

    @Binds
    abstract fun bindGetProfileTask(task: DefaultGetProfileInfoTask): GetProfileInfoTask

    @Binds
    abstract fun bindRefreshUserThreePidsTask(task: DefaultRefreshUserThreePidsTask): RefreshUserThreePidsTask

    @Binds
    abstract fun bindBindThreePidsTask(task: DefaultBindThreePidsTask): BindThreePidsTask

    @Binds
    abstract fun bindUnbindThreePidsTask(task: DefaultUnbindThreePidsTask): UnbindThreePidsTask

    @Binds
    abstract fun bindSetDisplayNameTask(task: DefaultSetDisplayNameTask): SetDisplayNameTask

    @Binds
    abstract fun bindSetAvatarUrlTask(task: DefaultSetAvatarUrlTask): SetAvatarUrlTask

    @Binds
    abstract fun bindAddThreePidTask(task: DefaultAddThreePidTask): AddThreePidTask

    @Binds
    abstract fun bindValidateSmsCodeTask(task: DefaultValidateSmsCodeTask): ValidateSmsCodeTask

    @Binds
    abstract fun bindFinalizeAddingThreePidTask(task: DefaultFinalizeAddingThreePidTask): FinalizeAddingThreePidTask

    @Binds
    abstract fun bindDeleteThreePidTask(task: DefaultDeleteThreePidTask): DeleteThreePidTask
}
