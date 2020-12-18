/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.core.utils.lsFiles
import timber.log.Timber

/**
 * Receiver to handle some command from ADB
 */
class DebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("Received debug action: ${intent.action}")

        intent.action?.let {
            when {
                it.endsWith(DEBUG_ACTION_DUMP_FILESYSTEM)    -> lsFiles(context)
                it.endsWith(DEBUG_ACTION_DUMP_PREFERENCES)   -> dumpPreferences(context)
                it.endsWith(DEBUG_ACTION_ALTER_SCALAR_TOKEN) -> alterScalarToken(context)
            }
        }
    }

    private fun dumpPreferences(context: Context) {
        logPrefs("DefaultSharedPreferences", DefaultSharedPreferences.getInstance(context))
    }

    private fun logPrefs(name: String, sharedPreferences: SharedPreferences?) {
        Timber.v("SharedPreferences $name:")

        sharedPreferences?.let { prefs ->
            prefs.all.keys.forEach { key ->
                Timber.v("$key : ${prefs.all[key]}")
            }
        }
    }

    private fun alterScalarToken(context: Context) {
        DefaultSharedPreferences.getInstance(context).edit {
            // putString("SCALAR_TOKEN_PREFERENCE_KEY" + Matrix.getInstance(context).defaultSession.myUserId, "bad_token")
        }
    }

    companion object {
        private const val DEBUG_ACTION_DUMP_FILESYSTEM = ".DEBUG_ACTION_DUMP_FILESYSTEM"
        private const val DEBUG_ACTION_DUMP_PREFERENCES = ".DEBUG_ACTION_DUMP_PREFERENCES"
        private const val DEBUG_ACTION_ALTER_SCALAR_TOKEN = ".DEBUG_ACTION_ALTER_SCALAR_TOKEN"

        fun getIntentFilter(context: Context) = IntentFilter().apply {
            addAction(context.packageName + DEBUG_ACTION_DUMP_FILESYSTEM)
            addAction(context.packageName + DEBUG_ACTION_DUMP_PREFERENCES)
            addAction(context.packageName + DEBUG_ACTION_ALTER_SCALAR_TOKEN)
        }
    }
}
