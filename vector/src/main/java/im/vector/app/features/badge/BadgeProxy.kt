/*
 * Copyright 2019 New Vector Ltd
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

@file:Suppress("UNUSED_PARAMETER")

package im.vector.app.features.badge

import android.content.Context
import android.os.Build
import me.leolin.shortcutbadger.ShortcutBadger
import org.matrix.android.sdk.api.session.Session

/**
 * Manage application badge (displayed in the launcher)
 */
object BadgeProxy {

    /**
     * Badge is now managed by notification channel, so no need to use compatibility library in recent versions
     *
     * @return true if library ShortcutBadger can be used
     */
    private fun useShortcutBadger() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    /**
     * Update the application badge value.
     *
     * @param context    the context
     * @param badgeValue the new badge value
     */
    fun updateBadgeCount(context: Context, badgeValue: Int) {
        if (!useShortcutBadger()) {
            return
        }

        ShortcutBadger.applyCount(context, badgeValue)
    }

    /**
     * Refresh the badge count for specific configurations.<br></br>
     * The refresh is only effective if the device is:
     *  * offline * does not support FCM
     *  * FCM registration failed
     * <br></br>Notifications rooms are parsed to track the notification count value.
     *
     * @param aSession session value
     * @param aContext App context
     */
    fun specificUpdateBadgeUnreadCount(aSession: Session?, aContext: Context?) {
        if (!useShortcutBadger()) {
            return
        }

        /* TODO
        val dataHandler: MXDataHandler

        // sanity check
        if (null == aContext || null == aSession) {
            Timber.w("## specificUpdateBadgeUnreadCount(): invalid input null values")
        } else {
            dataHandler = aSession.dataHandler

            if (dataHandler == null) {
                Timber.w("## specificUpdateBadgeUnreadCount(): invalid DataHandler instance")
            } else {
                if (aSession.isAlive) {
                    var isRefreshRequired: Boolean
                    val pushManager = Matrix.getInstance(aContext)!!.pushManager

                    // update the badge count if the device is offline, FCM is not supported or FCM registration failed
                    isRefreshRequired = !Matrix.getInstance(aContext)!!.isConnected
                    isRefreshRequired = isRefreshRequired or (null != pushManager && (!pushManager.useFcm() || !pushManager.hasRegistrationToken()))

                    if (isRefreshRequired) {
                        updateBadgeCount(aContext, dataHandler)
                    }
                }
            }
        }
         */
    }

    /**
     * Update the badge count value according to the rooms content.
     *
     * @param aContext     App context
     * @param aDataHandler data handler instance
     */
    private fun updateBadgeCount(aSession: Session?, aContext: Context?) {
        if (!useShortcutBadger()) {
            return
        }

        /* TODO
        //sanity check
        if (null == aContext || null == aDataHandler) {
            Timber.w("## updateBadgeCount(): invalid input null values")
        } else if (null == aDataHandler.store) {
            Timber.w("## updateBadgeCount(): invalid store instance")
        } else {
            val roomCompleteList = ArrayList(aDataHandler.store.rooms)
            var unreadRoomsCount = 0

            for (room in roomCompleteList) {
                if (room.notificationCount > 0) {
                    unreadRoomsCount++
                }
            }

            // update the badge counter
            Timber.v("## updateBadgeCount(): badge update count=$unreadRoomsCount")
            updateBadgeCount(aContext, unreadRoomsCount)
        }
         */
    }
}
