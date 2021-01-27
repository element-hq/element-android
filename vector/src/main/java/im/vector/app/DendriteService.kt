/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import gobind.DendriteMonolith

class DendriteService : Service() {
    private val binder = DendriteLocalBinder()
    private var monolith: DendriteMonolith? = null

    inner class DendriteLocalBinder : Binder() {
        fun getService() : DendriteService {
            return this@DendriteService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    fun registerUser(userID: String, password: String): String {
        return monolith?.registerUser(userID, password) ?: ""
    }

    override fun onCreate() {
        if (monolith == null) {
            monolith = gobind.DendriteMonolith()
        }

        Toast.makeText(applicationContext, "Starting Dendrite", Toast.LENGTH_SHORT).show()
        monolith?.storageDirectory = applicationContext.filesDir.toString()
        monolith?.start()

        super.onCreate()
    }

    override fun onDestroy() {
        if (monolith == null) {
            return
        }

        Toast.makeText(applicationContext, "Shutting down Dendrite", Toast.LENGTH_SHORT).show()
        monolith?.stop()
        monolith = null

        super.onDestroy()
    }
}
