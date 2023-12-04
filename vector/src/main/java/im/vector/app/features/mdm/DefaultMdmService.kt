/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.mdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMdmService @Inject constructor(
        @ApplicationContext applicationContext: Context
) : MdmService {
    private val restrictionsManager = applicationContext.getSystemService<RestrictionsManager>()
    private var onChangedListener: (() -> Unit)? = null

    private val restrictionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.w("Restrictions changed")
            onChangedListener?.invoke()
        }
    }

    override fun registerListener(context: Context, onChangedListener: () -> Unit) {
        val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        this.onChangedListener = onChangedListener
        context.registerReceiver(restrictionsReceiver, restrictionsFilter)
    }

    override fun unregisterListener(context: Context) {
        context.unregisterReceiver(restrictionsReceiver)
        this.onChangedListener = null
    }

    override fun getData(mdmData: MdmData): String? {
        return restrictionsManager?.applicationRestrictions?.getString(mdmData.key)
    }
}
