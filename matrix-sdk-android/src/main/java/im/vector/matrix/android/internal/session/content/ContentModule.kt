/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.matrix.android.internal.session.content

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.internal.session.DefaultSession
import org.koin.dsl.module.module

internal class ContentModule {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            DefaultContentUploadStateTracker() as ContentUploadStateTracker
        }

        scope(DefaultSession.SCOPE) {
            FileUploader(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            val sessionParams = get<SessionParams>()
            DefaultContentUrlResolver(sessionParams.homeServerConnectionConfig) as ContentUrlResolver
        }

    }


}
