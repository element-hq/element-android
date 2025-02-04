/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import im.vector.app.core.debug.DebugReceiver
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.utils.lsFiles
import timber.log.Timber
import javax.inject.Inject

/**
 * Receiver to handle some command from ADB
 */
class VectorDebugReceiver @Inject constructor(
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
) : BroadcastReceiver(), DebugReceiver {

    override fun register(context: Context) {
        ContextCompat.registerReceiver(
                context,
                this,
                getIntentFilter(context),
                ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("Received debug action: ${intent.action}")

        intent.action?.let {
            when {
                it.endsWith(DEBUG_ACTION_DUMP_FILESYSTEM) -> lsFiles(context)
                it.endsWith(DEBUG_ACTION_DUMP_PREFERENCES) -> dumpPreferences()
                it.endsWith(DEBUG_ACTION_ALTER_SCALAR_TOKEN) -> alterScalarToken()
            }
        }
    }

    private fun dumpPreferences() {
        logPrefs("DefaultSharedPreferences", sharedPreferences)
    }

    private fun logPrefs(name: String, sharedPreferences: SharedPreferences?) {
        Timber.v("SharedPreferences $name:")

        sharedPreferences?.let { prefs ->
            prefs.all.keys.forEach { key ->
                Timber.v("$key : ${prefs.all[key]}")
            }
        }
    }

    private fun alterScalarToken() {
        sharedPreferences.edit {
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
