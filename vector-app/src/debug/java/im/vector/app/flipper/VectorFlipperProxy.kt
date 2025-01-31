/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.flipper

import android.content.Context
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import com.kgurgul.flipper.RealmDatabaseDriver
import com.kgurgul.flipper.RealmDatabaseProvider
import im.vector.app.core.debug.FlipperProxy
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.Matrix
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VectorFlipperProxy @Inject constructor(
        private val context: Context,
) : FlipperProxy {
    private val networkFlipperPlugin = NetworkFlipperPlugin()

    override fun init(matrix: Matrix) {
        SoLoader.init(context, false)

        if (FlipperUtils.shouldEnableFlipper(context)) {
            val client = AndroidFlipperClient.getInstance(context)
            client.addPlugin(CrashReporterPlugin.getInstance())
            client.addPlugin(SharedPreferencesFlipperPlugin(context))
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
            client.addPlugin(networkFlipperPlugin)
            client.addPlugin(
                    DatabasesFlipperPlugin(
                            RealmDatabaseDriver(
                                    context = context,
                                    realmDatabaseProvider = object : RealmDatabaseProvider {
                                        override fun getRealmConfigurations(): List<RealmConfiguration> {
                                            return matrix.debugService().getAllRealmConfigurations()
                                        }
                                    })
                    )
            )
            client.start()
        }
    }

    override fun networkInterceptor() = FlipperOkhttpInterceptor(networkFlipperPlugin)
}
