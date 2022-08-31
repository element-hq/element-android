/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.nightly

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.time.Clock
import im.vector.app.features.home.NightlyProxy
import timber.log.Timber
import javax.inject.Inject

class FirebaseNightlyProxy @Inject constructor(
        private val clock: Clock,
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
        private val buildMeta: BuildMeta,
) : NightlyProxy {

    override fun onHomeResumed() {
        if (!canDisplayPopup()) return
        val firebaseAppDistribution = FirebaseAppDistribution.getInstance()
        firebaseAppDistribution.updateIfNewReleaseAvailable()
                .addOnProgressListener { up ->
                    Timber.d("FirebaseAppDistribution progress: ${up.updateStatus}. ${up.apkBytesDownloaded}/${up.apkFileTotalBytes}")
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAppDistributionException) {
                        when (e.errorCode) {
                            FirebaseAppDistributionException.Status.NOT_IMPLEMENTED -> {
                                // SDK did nothing. This is expected when building for Play.
                            }
                            else -> {
                                // Handle other errors.
                                Timber.e(e, "FirebaseAppDistribution error, status: ${e.errorCode}")
                            }
                        }
                    } else {
                        Timber.e(e, "FirebaseAppDistribution - other error")
                    }
                }
    }

    private fun canDisplayPopup(): Boolean {
        if (buildMeta.applicationId != "im.vector.app.nightly") return false
        val today = clock.epochMillis() / A_DAY_IN_MILLIS
        val lastDisplayPopupDay = sharedPreferences.getLong(SHARED_PREF_KEY, 0)
        return (today > lastDisplayPopupDay)
                .also { canDisplayPopup ->
                    if (canDisplayPopup) {
                        sharedPreferences.edit {
                            putLong(SHARED_PREF_KEY, today)
                        }
                    }
                }
    }

    companion object {
        private const val A_DAY_IN_MILLIS = 8_600_000L
        private const val SHARED_PREF_KEY = "LAST_NIGHTLY_POPUP_DAY"
    }
}
