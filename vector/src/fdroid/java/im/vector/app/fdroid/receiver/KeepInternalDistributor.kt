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

package im.vector.app.fdroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * UnifiedPush lib tracks an action to check installed and uninstalled distributors.
 * We declare it to keep the background sync as an internal unifiedpush distributor.
 * This class is used to declare this action.
 */
class KeepInternalDistributor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
