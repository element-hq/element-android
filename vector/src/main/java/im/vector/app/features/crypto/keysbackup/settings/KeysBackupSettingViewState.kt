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

package im.vector.app.features.crypto.keysbackup.settings

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult

data class KeysBackupSettingViewState(val keysBackupVersionTrust: Async<KeysBackupVersionTrust> = Uninitialized,
                                      val keysBackupState: KeysBackupState? = null,
                                      val keysBackupVersion: KeysVersionResult? = null,
                                      val deleteBackupRequest: Async<Unit> = Uninitialized) :
        MavericksState
