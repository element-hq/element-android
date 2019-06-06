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

package im.vector.riotredesign.features.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import butterknife.OnClick
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseActivity


class DebugMenuActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_debug_menu

    @OnClick(R.id.debug_test_text_view_link)
    fun testTextViewLink() {
        startActivity(Intent(this, TestLinkifyActivity::class.java))
    }

    @OnClick(R.id.debug_test_notification)
    fun testNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            "CHAN",
                            "Channel name",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )

            channel.description = "Channel description"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

            val channel2 =
                    NotificationChannel(
                            "CHAN2",
                            "Channel name 2",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )

            channel2.description = "Channel description 2"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel2)
        }


        val builder = NotificationCompat.Builder(this, "CHAN")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Title")
                .setContentText("Content")
                // No effect because it's a group summary notif
                .setNumber(33)
                .setSmallIcon(R.drawable.logo_transparent)
                // This provocate the badge issue: no badge for group notification
                .setGroup("GroupKey")
                .setGroupSummary(true)

        val messagingStyle1 = NotificationCompat.MessagingStyle(
                Person.Builder()
                        .setName("User name")
                        .build()
        )
                .addMessage("Message 1 - 1", System.currentTimeMillis(), Person.Builder().setName("user 1-1").build())
                .addMessage("Message 1 - 2", System.currentTimeMillis(), Person.Builder().setName("user 1-2").build())

        val messagingStyle2 = NotificationCompat.MessagingStyle(
                Person.Builder()
                        .setName("User name 2")
                        .build()
        )
                .addMessage("Message 2 - 1", System.currentTimeMillis(), Person.Builder().setName("user 1-1").build())
                .addMessage("Message 2 - 2", System.currentTimeMillis(), Person.Builder().setName("user 1-2").build())


        notificationManager.notify(10, builder.build())

        notificationManager.notify(
                11,
                NotificationCompat.Builder(this, "CHAN")
                        .setChannelId("CHAN")
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle("Title 1")
                        .setContentText("Content 1")
                        // For shortcut on long press on launcher icon
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setStyle(messagingStyle1)
                        .setSmallIcon(R.drawable.logo_transparent)
                        .setGroup("GroupKey")
                        .build()
        )

        notificationManager.notify(
                12,
                NotificationCompat.Builder(this, "CHAN2")
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle("Title 2")
                        .setContentText("Content 2")
                        .setStyle(messagingStyle2)
                        .setSmallIcon(R.drawable.logo_transparent)
                        .setGroup("GroupKey")
                        .build()
        )
    }
}

