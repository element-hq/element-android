/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import androidx.core.content.ContextCompat
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
        ContextCompat.registerReceiver(
                context,
                restrictionsReceiver,
                restrictionsFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun unregisterListener(context: Context) {
        context.unregisterReceiver(restrictionsReceiver)
        this.onChangedListener = null
    }

    override fun getData(mdmData: MdmData): String? {
        return restrictionsManager?.applicationRestrictions?.getString(mdmData.key)
    }
}
