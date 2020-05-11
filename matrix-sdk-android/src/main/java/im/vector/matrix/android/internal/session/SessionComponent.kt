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

package im.vector.matrix.android.internal.session

import dagger.BindsInstance
import dagger.Component
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.CancelGossipRequestWorker
import im.vector.matrix.android.internal.crypto.CryptoModule
import im.vector.matrix.android.internal.crypto.SendGossipRequestWorker
import im.vector.matrix.android.internal.crypto.SendGossipWorker
import im.vector.matrix.android.internal.crypto.verification.SendVerificationMessageWorker
import im.vector.matrix.android.internal.di.MatrixComponent
import im.vector.matrix.android.internal.di.SessionAssistedInjectModule
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.account.AccountModule
import im.vector.matrix.android.internal.session.cache.CacheModule
import im.vector.matrix.android.internal.session.content.ContentModule
import im.vector.matrix.android.internal.session.content.UploadContentWorker
import im.vector.matrix.android.internal.session.filter.FilterModule
import im.vector.matrix.android.internal.session.group.GetGroupDataWorker
import im.vector.matrix.android.internal.session.group.GroupModule
import im.vector.matrix.android.internal.session.homeserver.HomeServerCapabilitiesModule
import im.vector.matrix.android.internal.session.openid.OpenIdModule
import im.vector.matrix.android.internal.session.profile.ProfileModule
import im.vector.matrix.android.internal.session.pushers.AddHttpPusherWorker
import im.vector.matrix.android.internal.session.pushers.PushersModule
import im.vector.matrix.android.internal.session.room.RoomModule
import im.vector.matrix.android.internal.session.room.relation.SendRelationWorker
import im.vector.matrix.android.internal.session.room.send.EncryptEventWorker
import im.vector.matrix.android.internal.session.room.send.MultipleEventSendingDispatcherWorker
import im.vector.matrix.android.internal.session.room.send.RedactEventWorker
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.session.signout.SignOutModule
import im.vector.matrix.android.internal.session.sync.SyncModule
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.job.SyncWorker
import im.vector.matrix.android.internal.session.user.UserModule
import im.vector.matrix.android.internal.session.user.accountdata.AccountDataModule
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers

@Component(dependencies = [MatrixComponent::class],
        modules = [
            SessionModule::class,
            RoomModule::class,
            SyncModule::class,
            HomeServerCapabilitiesModule::class,
            SignOutModule::class,
            GroupModule::class,
            UserModule::class,
            FilterModule::class,
            GroupModule::class,
            ContentModule::class,
            CacheModule::class,
            CryptoModule::class,
            PushersModule::class,
            OpenIdModule::class,
            AccountDataModule::class,
            ProfileModule::class,
            SessionAssistedInjectModule::class,
            AccountModule::class
        ]
)
@SessionScope
internal interface SessionComponent {

    fun coroutineDispatchers(): MatrixCoroutineDispatchers

    fun session(): Session

    fun syncTask(): SyncTask

    fun syncTokenStore(): SyncTokenStore

    fun networkConnectivityChecker(): NetworkConnectivityChecker

    fun taskExecutor(): TaskExecutor

    fun inject(worker: SendEventWorker)

    fun inject(worker: SendRelationWorker)

    fun inject(worker: EncryptEventWorker)

    fun inject(worker: MultipleEventSendingDispatcherWorker)

    fun inject(worker: RedactEventWorker)

    fun inject(worker: GetGroupDataWorker)

    fun inject(worker: UploadContentWorker)

    fun inject(worker: SyncWorker)

    fun inject(worker: AddHttpPusherWorker)

    fun inject(worker: SendVerificationMessageWorker)

    fun inject(worker: SendGossipRequestWorker)

    fun inject(worker: CancelGossipRequestWorker)

    fun inject(worker: SendGossipWorker)

    @Component.Factory
    interface Factory {
        fun create(
                matrixComponent: MatrixComponent,
                @BindsInstance sessionParams: SessionParams): SessionComponent
    }
}
