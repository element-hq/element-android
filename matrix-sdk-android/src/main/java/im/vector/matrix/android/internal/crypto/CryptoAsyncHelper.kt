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

package im.vector.matrix.android.internal.crypto

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

private const val THREAD_CRYPTO_NAME = "Crypto_Thread"

// TODO Remove and replace by Task
internal object CryptoAsyncHelper {

    private var uiHandler: Handler? = null
    private var cryptoBackgroundHandler: Handler? = null

    fun getUiHandler(): Handler {
        return uiHandler
               ?: Handler(Looper.getMainLooper())
                       .also { uiHandler = it }
    }


    fun getDecryptBackgroundHandler(): Handler {
        return getCryptoBackgroundHandler()
    }

    fun getEncryptBackgroundHandler(): Handler {
        return getCryptoBackgroundHandler()
    }

    private fun getCryptoBackgroundHandler(): Handler {
        return cryptoBackgroundHandler
               ?: createCryptoBackgroundHandler()
                       .also { cryptoBackgroundHandler = it }
    }

    private fun createCryptoBackgroundHandler(): Handler {
        val handlerThread = HandlerThread(THREAD_CRYPTO_NAME)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }


}