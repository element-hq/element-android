/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.di

import android.content.Context
import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayer
import im.vector.app.features.voicebroadcast.listening.VoiceBroadcastPlayerImpl
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorder
import im.vector.app.features.voicebroadcast.recording.VoiceBroadcastRecorderQ
import im.vector.app.features.voicebroadcast.usecase.GetVoiceBroadcastStateEventLiveUseCase
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class VoiceModule {

    companion object {
        @Provides
        @Singleton
        fun providesVoiceBroadcastRecorder(
                context: Context,
                sessionHolder: ActiveSessionHolder,
                getVoiceBroadcastStateEventLiveUseCase: GetVoiceBroadcastStateEventLiveUseCase,
        ): VoiceBroadcastRecorder? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VoiceBroadcastRecorderQ(
                        context = context,
                        sessionHolder = sessionHolder,
                        getVoiceBroadcastEventUseCase = getVoiceBroadcastStateEventLiveUseCase
                )
            } else {
                null
            }
        }
    }

    @Binds
    abstract fun bindVoiceBroadcastPlayer(player: VoiceBroadcastPlayerImpl): VoiceBroadcastPlayer
}
