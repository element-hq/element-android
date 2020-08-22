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

package im.vector.app.features.debug

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.getSystemService
import butterknife.OnClick
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_LAUNCH_CAMERA
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.toast
import im.vector.app.features.debug.sas.DebugSasEmojiActivity
import im.vector.app.features.qrcode.QrCodeScannerActivity
import org.matrix.android.sdk.internal.crypto.verification.qrcode.toQrCodeData
import kotlinx.android.synthetic.debug.activity_debug_menu.*
import timber.log.Timber
import javax.inject.Inject

class DebugMenuActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_debug_menu

    @Inject
    lateinit var activeSessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    private lateinit var buffer: ByteArray

    override fun initUiAndData() {
        // renderQrCode("https://www.example.org")

        buffer = ByteArray(256)
        for (i in buffer.indices) {
            buffer[i] = i.toByte()
        }

        val string = buffer.toString(Charsets.ISO_8859_1)

        renderQrCode(string)
    }

    private fun renderQrCode(text: String) {
        debug_qr_code.setData(text, true)
    }

    @OnClick(R.id.debug_test_text_view_link)
    fun testTextViewLink() {
        startActivity(Intent(this, TestLinkifyActivity::class.java))
    }

    @OnClick(R.id.debug_show_sas_emoji)
    fun showSasEmoji() {
        startActivity(Intent(this, DebugSasEmojiActivity::class.java))
    }

    @OnClick(R.id.debug_test_notification)
    fun testNotification() {
        val notificationManager = getSystemService<NotificationManager>()!!

        // Create channel first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            "CHAN",
                            "Channel name",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )

            channel.description = "Channel description"
            notificationManager.createNotificationChannel(channel)

            val channel2 =
                    NotificationChannel(
                            "CHAN2",
                            "Channel name 2",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )

            channel2.description = "Channel description 2"
            notificationManager.createNotificationChannel(channel2)
        }

        val builder = NotificationCompat.Builder(this, "CHAN")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Title")
                .setContentText("Content")
                // No effect because it's a group summary notif
                .setNumber(33)
                .setSmallIcon(R.drawable.ic_status_bar)
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
                        .setSmallIcon(R.drawable.ic_status_bar)
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
                        .setSmallIcon(R.drawable.ic_status_bar)
                        .setGroup("GroupKey")
                        .build()
        )
    }

    @OnClick(R.id.debug_test_material_theme_light)
    fun testMaterialThemeLight() {
        startActivity(Intent(this, DebugMaterialThemeLightActivity::class.java))
    }

    @OnClick(R.id.debug_test_material_theme_dark)
    fun testMaterialThemeDark() {
        startActivity(Intent(this, DebugMaterialThemeDarkActivity::class.java))
    }

    @OnClick(R.id.debug_test_crash)
    fun testCrash() {
        throw RuntimeException("Application crashed from user demand")
    }

    @OnClick(R.id.debug_scan_qr_code)
    fun scanQRCode() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
            doScanQRCode()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE_LAUNCH_CAMERA && allGranted(grantResults)) {
            doScanQRCode()
        }
    }

    private fun doScanQRCode() {
        QrCodeScannerActivity.startForResult(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                QrCodeScannerActivity.QR_CODE_SCANNER_REQUEST_CODE -> {
                    toast("QrCode: " + QrCodeScannerActivity.getResultText(data) + " is QRCode: " + QrCodeScannerActivity.getResultIsQrCode(data))

                    // Also update the current QR Code (reverse operation)
                    // renderQrCode(QrCodeScannerActivity.getResultText(data) ?: "")
                    val result = QrCodeScannerActivity.getResultText(data)!!

                    val qrCodeData = result.toQrCodeData()
                    Timber.e("qrCodeData: $qrCodeData")

                    if (result.length != buffer.size) {
                        Timber.e("Error, length are not the same")
                    } else {
                        // Convert to ByteArray
                        val byteArrayResult = result.toByteArray(Charsets.ISO_8859_1)
                        for (i in byteArrayResult.indices) {
                            if (buffer[i] != byteArrayResult[i]) {
                                Timber.e("Error for byte $i, expecting ${buffer[i]} and get ${byteArrayResult[i]}")
                            }
                        }
                    }
                }
            }
        }
    }
}
