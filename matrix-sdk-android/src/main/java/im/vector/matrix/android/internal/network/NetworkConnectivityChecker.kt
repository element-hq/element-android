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

package im.vector.matrix.android.internal.network

import android.content.Context
import com.novoda.merlin.Merlin
import com.novoda.merlin.MerlinsBeard
import im.vector.matrix.android.internal.di.MatrixScope
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@MatrixScope
internal class NetworkConnectivityChecker @Inject constructor(context: Context) {

    private val merlin = Merlin.Builder()
            .withConnectableCallbacks()
            .withDisconnectableCallbacks()
            .build(context)

    private val merlinsBeard = MerlinsBeard.Builder().build(context)
    private val listeners = Collections.synchronizedList(ArrayList<Listener>())

    init {
        merlin.bind()
        merlin.registerDisconnectable {
            Timber.v("On Disconnect")
            listeners.forEach {
                it.onDisconnect()
            }
        }
        merlin.registerConnectable {
            Timber.v("On Connect")
            listeners.forEach {
                it.onConnect()
            }
        }
    }

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    fun isConnected(): Boolean {
        return merlinsBeard.isConnected
    }

    interface Listener {
        fun onConnect() {

        }

        fun onDisconnect() {

        }
    }


}

