/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.media.domain.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.usecase.VectorBaseInOutUseCase
import im.vector.app.core.utils.saveMedia
import im.vector.app.features.notifications.NotificationUtils
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import java.io.File
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
        @ApplicationContext private val appContext: Context,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val notificationUtils: NotificationUtils
) : VectorBaseInOutUseCase<File, Unit> {

    // TODO
    //  what about UseCase Interface enforcing single type input? => no interface
    //  add unit tests
    //  PR to template structure of a class for discussion

    override suspend fun execute(input: File): Result<Unit> = withContext(coroutineDispatchers.io) {
        runCatching {
            saveMedia(
                    context = appContext,
                    file = input,
                    title = input.name,
                    mediaMimeType = getMimeTypeFromUri(appContext, input.toUri()),
                    notificationUtils = notificationUtils
            )
        }
    }
}
