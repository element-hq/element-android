/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media.domain.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.utils.saveMedia
import im.vector.app.features.notifications.NotificationUtils
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import java.io.File
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
        @ApplicationContext private val appContext: Context,
        private val session: Session,
        private val notificationUtils: NotificationUtils,
        private val clock: Clock,
) {

    suspend fun execute(input: File): Result<Unit> = withContext(session.coroutineDispatchers.io) {
        runCatching {
            saveMedia(
                    context = appContext,
                    file = input,
                    title = input.name,
                    mediaMimeType = getMimeTypeFromUri(appContext, input.toUri()),
                    notificationUtils = notificationUtils,
                    currentTimeMillis = clock.epochMillis()
            )
        }
    }
}
