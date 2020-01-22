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

package im.vector.riotx.features.debug

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import butterknife.OnClick
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.qrcode.toQrCode
import im.vector.riotx.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.riotx.core.utils.PERMISSION_REQUEST_CODE_LAUNCH_CAMERA
import im.vector.riotx.core.utils.allGranted
import im.vector.riotx.core.utils.checkPermissions
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.debug.sas.DebugSasEmojiActivity
import im.vector.riotx.features.qrcode.QrCodeScannerActivity
import kotlinx.android.synthetic.debug.activity_debug_menu.*
import javax.inject.Inject

class DebugMenuActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_debug_menu

    @Inject
    lateinit var activeSessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        renderQrCode("https://www.example.org")
    }

    private fun renderQrCode(text: String) {
        val qrBitmap = text.toQrCode()
        debug_qr_code.setImageBitmap(qrBitmap)
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

    @OnClick(R.id.debug_initialise_xsigning)
    fun testXSigning() {
        activeSessionHolder.getActiveSession().getCrossSigningService().initializeCrossSigning(null, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                if (failure is Failure.OtherServerError
                        && failure.httpCode == 401
                ) {
                    try {
                        MoshiProvider.providesMoshi()
                                .adapter(RegistrationFlowResponse::class.java)
                                .fromJson(failure.errorBody)
                    } catch (e: Exception) {
                        null
                    }?.let {
                        // Retry with authentication
                        if (it.flows?.any { it.stages?.contains(LoginFlowTypes.PASSWORD) == true } == true) {
                            // Ask for password
                            val inflater = this@DebugMenuActivity.layoutInflater
                            val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)

                            val input = layout.findViewById<EditText>(R.id.edit_text).also {
                                it.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                            }

                            val activeSession = activeSessionHolder.getActiveSession()
                            AlertDialog.Builder(this@DebugMenuActivity)
                                    .setTitle("Confirm password")
                                    .setView(layout)
                                    .setPositiveButton(R.string.ok) { _, _ ->
                                        val pass = input.text.toString()

                                        activeSession.getCrossSigningService().initializeCrossSigning(
                                                UserPasswordAuth(
                                                        session = it.session,
                                                        user = activeSession.myUserId,
                                                        password = pass
                                                )
                                        )
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                        } else {
                            // can't do this from here
                            AlertDialog.Builder(this@DebugMenuActivity)
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage("You cannot do that from mobile")
                                    .setPositiveButton(R.string.ok, null)
                                    .show()
                        }
                    }
                }
            }
        })
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
                    renderQrCode(QrCodeScannerActivity.getResultText(data) ?: "")
                }
            }
        }
    }
}
