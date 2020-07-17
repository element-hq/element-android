/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.common

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixComponent
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.android.internal.di.NetworkModule

@Component(modules = [TestModule::class, MatrixModule::class, NetworkModule::class, AuthModule::class, TestNetworkModule::class])
@MatrixScope
internal interface TestMatrixComponent : MatrixComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context,
                   @BindsInstance matrixConfiguration: MatrixConfiguration): TestMatrixComponent
    }
}
