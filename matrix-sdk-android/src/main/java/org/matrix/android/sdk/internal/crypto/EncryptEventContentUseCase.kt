// /*
// * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package org.matrix.android.sdk.internal.crypto
//
// import org.matrix.android.sdk.api.logger.LoggerTag
// import org.matrix.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
// import org.matrix.android.sdk.api.session.events.model.Content
// import org.matrix.android.sdk.api.session.events.model.EventType
// import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
// import org.matrix.android.sdk.internal.util.time.Clock
// import timber.log.Timber
// import javax.inject.Inject
//
// private val loggerTag = LoggerTag("EncryptEventContentUseCase", LoggerTag.CRYPTO)
//
// internal class EncryptEventContentUseCase @Inject constructor(
//        private val olmDevice: MXOlmDevice,
//        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
//        private val clock: Clock) {
//
//    suspend operator fun invoke(
//            eventContent: Content,
//            eventType: String,
//            roomId: String): MXEncryptEventContentResult {
//        val t0 = clock.epochMillis()
//        ensureOlmSessionsForDevicesAction.handle()
//        prepareToEncrypt(roomId, ensureAllMembersAreLoaded = false)
//        val content = olmMachine.encrypt(roomId, eventType, eventContent)
//        Timber.tag(loggerTag.value).v("## CRYPTO | encryptEventContent() : succeeds after ${clock.epochMillis() - t0} ms")
//        return MXEncryptEventContentResult(content, EventType.ENCRYPTED)
//    }
// }
