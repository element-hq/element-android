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

import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import timber.log.Timber
import javax.inject.Inject

class NightlyProxy @Inject constructor() {
    fun updateIfNewReleaseAvailable() {
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
}
