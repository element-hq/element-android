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

private const val THREAD_ENCRYPT_NAME = "Crypto_Encrypt_Thread"
private const val THREAD_DECRYPT_NAME = "Crypto_Decrypt_Thread"

internal object CryptoAsyncHelper {

    private var uiHandler: Handler? = null
    private var decryptBackgroundHandler: Handler? = null
    private var encryptBackgroundHandler: Handler? = null

    fun getUiHandler(): Handler {
        return uiHandler
                ?: Handler(Looper.getMainLooper())
                        .also { uiHandler = it }
    }

    fun getDecryptBackgroundHandler(): Handler {
        return decryptBackgroundHandler
                ?: createDecryptBackgroundHandler()
                        .also { decryptBackgroundHandler = it }
    }

    fun getEncryptBackgroundHandler(): Handler {
        return encryptBackgroundHandler
                ?: createEncryptBackgroundHandler()
                        .also { encryptBackgroundHandler = it }
    }

    private fun createDecryptBackgroundHandler(): Handler {
        val handlerThread = HandlerThread(THREAD_DECRYPT_NAME)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    private fun createEncryptBackgroundHandler(): Handler {
        val handlerThread = HandlerThread(THREAD_ENCRYPT_NAME)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

}