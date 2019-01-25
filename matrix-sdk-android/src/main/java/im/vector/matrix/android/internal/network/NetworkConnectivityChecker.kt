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
import com.novoda.merlin.registerable.connection.Connectable

internal class NetworkConnectivityChecker(context: Context) {

    private val merlin = Merlin.Builder().withConnectableCallbacks().build(context)
    private val merlinsBeard = MerlinsBeard.from(context)

    private val listeners = ArrayList<Listener>()

    fun register(listener: Listener) {
        if (listeners.isEmpty()) {
            merlin.bind()
        }
        listeners.add(listener)
        val connectable = Connectable {
            if (listeners.contains(listener)) {
                listener.onConnect()
            }
        }
        merlin.registerConnectable(connectable)
    }

    fun unregister(listener: Listener) {
        if (listeners.remove(listener) && listeners.isEmpty()) {
            merlin.unbind()
        }
    }

    fun isConnected(): Boolean {
        return merlinsBeard.isConnected
    }

    interface Listener {
        fun onConnect()
    }


}

