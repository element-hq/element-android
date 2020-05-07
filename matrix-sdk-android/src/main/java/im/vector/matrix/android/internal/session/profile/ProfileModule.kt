/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.profile

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.session.profile.ProfileService
import im.vector.matrix.android.internal.session.SessionScope
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
}
