/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.app.core.pushers

import android.app.AlertDialog
import android.content.Context
import android.widget.TextView
import androidx.core.content.edit
import im.vector.app.core.di.DefaultSharedPreferences
import org.unifiedpush.android.connector.Registration
import timber.log.Timber

/**
 * This class store the UnifiedPush Endpoint in SharedPrefs and ensure this token is retrieved.
 * It has an alter ego in the fdroid variant.
 */
object UPHelper {
    private val PREFS_UP_ENDPOINT = "UP_ENDPOINT"
    private val PREFS_PUSH_GATEWAY = "PUSH_GATEWAY"

    /**
     * Retrieves the UnifiedPush Endpoint.
     *
     * @return the UnifiedPush Endpoint or null if not received
     */
    fun getUpEndpoint(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_UP_ENDPOINT, null)
    }

    /**
     * Store UnifiedPush Endpoint to the SharedPrefs
     * TODO Store in realm
     *
     * @param context android context
     * @param endpoint the endpoint to store
     */
    fun storeUpEndpoint(context: Context,
                        endpoint: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_UP_ENDPOINT, endpoint)
        }
    }

    /**
     * Retrieves the Push Gateway.
     *
     * @return the Push Gateway or null if not defined
     */
    fun getPushGateway(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_PUSH_GATEWAY, null)
    }

    /**
     * Store Push Gateway to the SharedPrefs
     * TODO Store in realm
     *
     * @param context android context
     * @param gateway the push gateway to store
     */
    fun storePushGateway(context: Context,
                         gateway: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_PUSH_GATEWAY, gateway)
        }
    }

    fun registerUnifiedPush(context: Context) {
        val up = Registration()
        if (up.getDistributor(context).isNotEmpty()) {
            up.registerApp(context)
        }
        val distributors = up.getDistributors(context).toMutableList()
        /**
         * TODO: Check if it is the gplay flavour AND GServices are not available
         */
        if (false) {
            distributors.remove(context.packageName)
        }
        when(distributors.size){
            0 -> {
                /**
                 * TODO: fallback with sync service
                 */
            }
            1 -> {
                up.saveDistributor(context, distributors.first())
                up.registerApp(context)
            }
            else ->{
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setTitle("Choose a distributor")

                val distributorsArray = distributors.toTypedArray()
                builder.setItems(distributorsArray) { _, which ->
                    val distributor = distributorsArray[which]
                    up.saveDistributor(context, distributor)
                    Timber.i("Saving distributor: $distributor")
                    up.registerApp(context)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    fun reRegisterUnifiedPush(context: Context) {
        val up = Registration()
        up.safeRemoveDistributor(context)
        registerUnifiedPush(context)
    }
}
