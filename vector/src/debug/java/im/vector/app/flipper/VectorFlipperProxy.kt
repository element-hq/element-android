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
