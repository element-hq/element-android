/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.crypto.util

import androidx.annotation.DrawableRes
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel

@DrawableRes
fun RoomEncryptionTrustLevel?.toImageRes(): Int {
    return when (this) {
        null                             -> 0
        RoomEncryptionTrustLevel.Default -> R.drawable.ic_shield_black
        RoomEncryptionTrustLevel.Warning -> R.drawable.ic_shield_warning
        RoomEncryptionTrustLevel.Trusted -> R.drawable.ic_shield_trusted
    }.exhaustive
}
