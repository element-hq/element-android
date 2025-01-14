/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

/**
 * This class manages the sound ringtone for calls.
 * It allows you to use the default Element Ringtone, or the standard ringtone or set a different one from the available choices
 * in Android.
 */
class RingtoneUtils @Inject constructor(
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
        private val context: Context,
) {
    /**
     * Returns a Uri object that points to a specific Ringtone.
     *
     * If no Ringtone was explicitly set using Riot, it will return the Uri for the current system
     * ringtone for calls.
     *
     * @return the [Uri] of the currently set [Ringtone]
     * @see Ringtone
     */
    fun getCallRingtoneUri(): Uri? {
        val callRingtone: String? = sharedPreferences
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
    fun getCallRingtone(): Ringtone? {
        getCallRingtoneUri()?.let {
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
    fun getCallRingtoneName(): String? {
        return getCallRingtone()?.getTitle(context)
    }

    /**
     * Sets the selected ringtone for riot calls.
     *
     * @param ringtoneUri
     * @see Ringtone
     */
    fun setCallRingtoneUri(ringtoneUri: Uri) {
        sharedPreferences
                .edit {
                    putString(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY, ringtoneUri.toString())
                }
    }

    /**
     * Set using Riot default ringtone.
     */
    fun useRiotDefaultRingtone(): Boolean {
        return sharedPreferences.getBoolean(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY, true)
    }

    /**
     * Ask if default Riot ringtone has to be used.
     */
    fun setUseRiotDefaultRingtone(useRiotDefault: Boolean) {
        sharedPreferences
                .edit {
                    putBoolean(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY, useRiotDefault)
                }
    }
}
