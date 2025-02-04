/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.toast
import im.vector.app.features.debug.analytics.DebugAnalyticsActivity
import im.vector.app.features.debug.features.DebugFeaturesSettingsActivity
import im.vector.app.features.debug.jitsi.DebugJitsiActivity
import im.vector.app.features.debug.leak.DebugMemoryLeaksActivity
import im.vector.app.features.debug.sas.DebugSasEmojiActivity
import im.vector.app.features.debug.settings.DebugPrivateSettingsActivity
import im.vector.app.features.qrcode.QrCodeScannerActivity
import im.vector.application.databinding.ActivityDebugMenuBinding
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.ui.styles.debug.DebugMaterialThemeDarkDefaultActivity
import im.vector.lib.ui.styles.debug.DebugMaterialThemeDarkTestActivity
import im.vector.lib.ui.styles.debug.DebugMaterialThemeDarkVectorActivity
import im.vector.lib.ui.styles.debug.DebugMaterialThemeLightDefaultActivity
import im.vector.lib.ui.styles.debug.DebugMaterialThemeLightTestActivity
import im.vector.lib.ui.styles.debug.DebugMaterialThemeLightVectorActivity
import im.vector.lib.ui.styles.debug.DebugVectorButtonStylesDarkActivity
import im.vector.lib.ui.styles.debug.DebugVectorButtonStylesLightActivity
import im.vector.lib.ui.styles.debug.DebugVectorTextViewDarkActivity
import im.vector.lib.ui.styles.debug.DebugVectorTextViewLightActivity
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DebugMenuActivity : VectorBaseActivity<ActivityDebugMenuBinding>() {

    override fun getBinding() = ActivityDebugMenuBinding.inflate(layoutInflater)

    @Inject lateinit var clock: Clock

    private lateinit var buffer: ByteArray

    override fun initUiAndData() {
        // renderQrCode("https://www.example.org")

        buffer = ByteArray(256)
        for (i in buffer.indices) {
            buffer[i] = i.toByte()
        }

        val string = buffer.toString(Charsets.ISO_8859_1)

        renderQrCode(string)
        setupViews()
    }

    private fun setupViews() {
        views.debugFeatures.setOnClickListener { startActivity(Intent(this, DebugFeaturesSettingsActivity::class.java)) }
        views.debugPrivateSetting.setOnClickListener { openPrivateSettings() }
        views.debugAnalytics.setOnClickListener {
            startActivity(Intent(this, DebugAnalyticsActivity::class.java))
        }
        views.debugMemoryLeaks.setOnClickListener { openMemoryLeaksSettings() }
        views.debugTestTextViewLink.setOnClickListener { testTextViewLink() }
        views.debugOpenButtonStylesLight.setOnClickListener {
            startActivity(Intent(this, DebugVectorButtonStylesLightActivity::class.java))
        }
        views.debugOpenButtonStylesDark.setOnClickListener {
            startActivity(Intent(this, DebugVectorButtonStylesDarkActivity::class.java))
        }
        views.debugTestTextViewLight.setOnClickListener {
            startActivity(Intent(this, DebugVectorTextViewLightActivity::class.java))
        }
        views.debugTestTextViewDark.setOnClickListener {
            startActivity(Intent(this, DebugVectorTextViewDarkActivity::class.java))
        }
        views.debugShowSasEmoji.setOnClickListener { showSasEmoji() }
        views.debugTestNotification.setOnClickListener { testNotification() }
        views.debugTestMaterialThemeLightDefault.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeLightDefaultActivity::class.java))
        }
        views.debugTestMaterialThemeLightTest.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeLightTestActivity::class.java))
        }
        views.debugTestMaterialThemeLightVector.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeLightVectorActivity::class.java))
        }
        views.debugTestMaterialThemeDarkDefault.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeDarkDefaultActivity::class.java))
        }
        views.debugTestMaterialThemeDarkTest.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeDarkTestActivity::class.java))
        }
        views.debugTestMaterialThemeDarkVector.setOnClickListener {
            startActivity(Intent(this, DebugMaterialThemeDarkVectorActivity::class.java))
        }
        views.debugTestCrash.setOnClickListener { testCrash() }
        views.debugScanQrCode.setOnClickListener { scanQRCode() }
        views.debugPermission.setOnClickListener {
            startActivity(Intent(this, DebugPermissionActivity::class.java))
        }
        views.debugJitsi.setOnClickListener {
            startActivity(Intent(this, DebugJitsiActivity::class.java))
        }
    }

    private fun openPrivateSettings() {
        startActivity(Intent(this, DebugPrivateSettingsActivity::class.java))
    }

    private fun openMemoryLeaksSettings() {
        startActivity(Intent(this, DebugMemoryLeaksActivity::class.java))
    }

    private fun renderQrCode(text: String) {
        views.debugQrCode.setData(text)
    }

    private fun testTextViewLink() {
        startActivity(Intent(this, TestLinkifyActivity::class.java))
    }

    private fun showSasEmoji() {
        startActivity(Intent(this, DebugSasEmojiActivity::class.java))
    }

    private fun testNotification() {
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
                .setWhen(clock.epochMillis())
                .setContentTitle("Title")
                .setContentText("Content")
                // No effect because it's a group summary notif
                .setNumber(33)
                .setSmallIcon(R.drawable.ic_notification)
                // This provocate the badge issue: no badge for group notification
                .setGroup("GroupKey")
                .setGroupSummary(true)

        val messagingStyle1 = NotificationCompat.MessagingStyle(
                Person.Builder()
                        .setName("User name")
                        .build()
        )
                .addMessage("Message 1 - 1", clock.epochMillis(), Person.Builder().setName("user 1-1").build())
                .addMessage("Message 1 - 2", clock.epochMillis(), Person.Builder().setName("user 1-2").build())

        val messagingStyle2 = NotificationCompat.MessagingStyle(
                Person.Builder()
                        .setName("User name 2")
                        .build()
        )
                .addMessage("Message 2 - 1", clock.epochMillis(), Person.Builder().setName("user 1-1").build())
                .addMessage("Message 2 - 2", clock.epochMillis(), Person.Builder().setName("user 1-2").build())

        notificationManager.notify(10, builder.build())

        notificationManager.notify(
                11,
                NotificationCompat.Builder(this, "CHAN")
                        .setChannelId("CHAN")
                        .setWhen(clock.epochMillis())
                        .setContentTitle("Title 1")
                        .setContentText("Content 1")
                        // For shortcut on long press on launcher icon
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setStyle(messagingStyle1)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setGroup("GroupKey")
                        .build()
        )

        notificationManager.notify(
                12,
                NotificationCompat.Builder(this, "CHAN2")
                        .setWhen(clock.epochMillis())
                        .setContentTitle("Title 2")
                        .setContentText("Content 2")
                        .setStyle(messagingStyle2)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setGroup("GroupKey")
                        .build()
        )
    }

    private fun testCrash() {
        throw RuntimeException("Application crashed from user demand")
    }

    private fun scanQRCode() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, permissionCameraLauncher)) {
            doScanQRCode()
        }
    }

    private val permissionCameraLauncher = registerForPermissionsResult { allGranted, _ ->
        if (allGranted) {
            doScanQRCode()
        }
    }

    private fun doScanQRCode() {
        QrCodeScannerActivity.startForResult(this, qrStartForActivityResult)
    }

    private val qrStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            toast(
                    "QrCode: " + QrCodeScannerActivity.getResultText(activityResult.data) +
                            " is QRCode: " + QrCodeScannerActivity.getResultIsQrCode(activityResult.data)
            )

            // Also update the current QR Code (reverse operation)
            // renderQrCode(QrCodeScannerActivity.getResultText(data) ?: "")
            val result = QrCodeScannerActivity.getResultText(activityResult.data)!!

            val qrCodeData = null // This is now internal: result.toQrCodeData()
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
            // Ensure developer will see that this cannot work anymore
            error("toQrCodeData() is now internal")
        }
    }
}
