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

package im.vector.matrix.android.internal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.internal.crypto.CryptoAsyncHelper
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher

@Module
internal class MatrixModule(private val context: Context) {

    @Provides
    @MatrixScope
    fun providesContext(): Context {
            return context
        }

    @Provides
    @MatrixScope
    fun providesMatrixCoroutineDispatchers(): MatrixCoroutineDispatchers {
            val cryptoHandler = CryptoAsyncHelper.getDecryptBackgroundHandler()
            return MatrixCoroutineDispatchers(io = Dispatchers.IO,
                    computation = Dispatchers.IO,
                    main = Dispatchers.Main,
                    crypto = cryptoHandler.asCoroutineDispatcher("crypto")
            )
        }

}