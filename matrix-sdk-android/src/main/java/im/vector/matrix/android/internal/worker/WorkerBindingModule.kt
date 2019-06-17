/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.worker

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import im.vector.matrix.android.internal.session.content.UploadContentWorker
import im.vector.matrix.android.internal.session.group.GetGroupDataWorker
import im.vector.matrix.android.internal.session.room.relation.SendRelationWorker
import im.vector.matrix.android.internal.session.room.send.EncryptEventWorker
import im.vector.matrix.android.internal.session.room.send.RedactEventWorker
import im.vector.matrix.android.internal.session.room.send.SendEventWorker


@Module
internal interface WorkerBindingModule {

    @Binds
    @IntoMap
    @WorkerKey(SendEventWorker::class)
    fun bindSendEventWorker(factory: SendEventWorker.Factory): DelegateWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(SendRelationWorker::class)
    fun bindSendRelationWorker(factory: SendRelationWorker.Factory): DelegateWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(EncryptEventWorker::class)
    fun bindEncryptEventWorker(factory: EncryptEventWorker.Factory): DelegateWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(RedactEventWorker::class)
    fun bindRedactEventWorker(factory: RedactEventWorker.Factory): DelegateWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(GetGroupDataWorker::class)
    fun bindGetGroupDataWorker(factory: GetGroupDataWorker.Factory): DelegateWorkerFactory

    @Binds
    @IntoMap
    @WorkerKey(UploadContentWorker::class)
    fun bindUploadContentWorker(factory: UploadContentWorker.Factory): DelegateWorkerFactory
}