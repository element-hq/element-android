/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.core.utils

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.edit
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.features.settings.VectorPreferences

/**
 * This file manages the sound ringtone for calls.
 * It allows you to use the default Riot Ringtone, or the standard ringtone or set a different one from the available choices
 * in Android.
 */

/**
 * Returns a Uri object that points to a specific Ringtone.
 *
 * If no Ringtone was explicitly set using Riot, it will return the Uri for the current system
 * ringtone for calls.
 *
 * @return the [Uri] of the currently set [Ringtone]
 * @see Ringtone
 */
fun getCallRingtoneUri(context: Context): Uri? {
    val callRingtone: String? = DefaultSharedPreferences.getInstance(context)
            .getString(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY, null)

    callRingtone?.let {
        return Uri.parse(it)
    }

    return try {
        // Use current system notification sound for incoming calls per default (note that it can return null)
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
    } catch (e: SecurityException) {
        // Ignore for now
        null
    }
}

/**
 * Returns a Ringtone object that can then be played.
 *
 * If no Ringtone was explicitly set using Riot, it will return the current system ringtone
 * for calls.
 *
 * @return the currently set [Ringtone]
 * @see Ringtone
 */
fun getCallRingtone(context: Context): Ringtone? {
    getCallRingtoneUri(context)?.let {
        // Note that it can also return null
        return RingtoneManager.getRingtone(context, it)
    }

    return null
}

/**
 * Returns a String with the name of the current Ringtone.
 *
 * If no Ringtone was explicitly set using Riot, it will return the name of the current system
 * ringtone for calls.
 *
 * @return the name of the currently set [Ringtone], or null
 * @see Ringtone
 */
fun getCallRingtoneName(context: Context): String? {
    return getCallRingtone(context)?.getTitle(context)
}

/**
 * Sets the selected ringtone for riot calls.
 *
 * @param ringtoneUri
 * @see Ringtone
 */
fun setCallRingtoneUri(context: Context, ringtoneUri: Uri) {
    DefaultSharedPreferences.getInstance(context)
            .edit {
                putString(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY, ringtoneUri.toString())
            }
}

/**
 * Set using Riot default ringtone
 */
fun useRiotDefaultRingtone(context: Context): Boolean {
    return DefaultSharedPreferences.getInstance(context).getBoolean(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY, true)
}

/**
 * Ask if default Riot ringtone has to be used
 */
fun setUseRiotDefaultRingtone(context: Context, useRiotDefault: Boolean) {
    DefaultSharedPreferences.getInstance(context)
            .edit {
                putBoolean(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY, useRiotDefault)
            }
}
