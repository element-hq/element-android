/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.isVisible
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityWidgetBinding
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionViewEvents
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionViewModel
import im.vector.lib.core.utils.compat.getParcelableCompat
import im.vector.lib.core.utils.compat.getSerializableCompat
import im.vector.lib.strings.CommonStrings
import im.vector.lib.ui.styles.R
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Content
import java.io.Serializable

@AndroidEntryPoint
class WidgetActivity : VectorBaseActivity<ActivityWidgetBinding>() {

    companion object {
        private const val WIDGET_FRAGMENT_TAG = "WIDGET_FRAGMENT_TAG"
        private const val WIDGET_PERMISSION_FRAGMENT_TAG = "WIDGET_PERMISSION_FRAGMENT_TAG"
        private const val EXTRA_RESULT = "EXTRA_RESULT"
        private const val REQUEST_CODE_HANGUP = 1
        private const val ACTION_MEDIA_CONTROL = "MEDIA_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "EXTRA_CONTROL_TYPE"
        private const val CONTROL_TYPE_HANGUP = 2

        fun newIntent(context: Context, args: WidgetArgs): Intent {
            return Intent(context, WidgetActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun getOutput(intent: Intent): Content? {
            return intent.extras?.getSerializableCompat(EXTRA_RESULT) as? Content
        }

        private fun createResultIntent(content: Content): Intent {
            return Intent().apply {
                putExtra(EXTRA_RESULT, content as Serializable)
            }
        }
    }

    private val viewModel: WidgetViewModel by viewModel()
    private val permissionViewModel: RoomWidgetPermissionViewModel by viewModel()

    override fun getBinding() = ActivityWidgetBinding.inflate(layoutInflater)

    override fun getTitleRes() = CommonStrings.room_widget_activity_title

    override fun initUiAndData() {
        val widgetArgs: WidgetArgs? = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG)
        if (widgetArgs == null) {
            finish()
            return
        }
        setupToolbar(views.toolbar)
                .allowBack()
        views.toolbar.isVisible = widgetArgs.kind.nameRes != 0
        viewModel.observeViewEvents {
            when (it) {
                is WidgetViewEvents.Close -> handleClose(it)
                else -> Unit
            }
        }

        // Trust element call widget by default
        if (widgetArgs.kind == WidgetKind.ELEMENT_CALL && vectorPreferences.labsEnableElementCallPermissionShortcuts()) {
            if (supportFragmentManager.findFragmentByTag(WIDGET_FRAGMENT_TAG) == null) {
                addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)
                addFragment(views.fragmentContainer, WidgetFragment::class.java, widgetArgs, WIDGET_FRAGMENT_TAG)
            }
        } else {
            permissionViewModel.observeViewEvents {
                when (it) {
                    is RoomWidgetPermissionViewEvents.Close -> finish()
                }
            }

            viewModel.onEach(WidgetViewState::status) { ws ->
                when (ws) {
                    WidgetStatus.UNKNOWN -> {
                    }
                    WidgetStatus.WIDGET_NOT_ALLOWED -> {
                        val dFrag = supportFragmentManager.findFragmentByTag(WIDGET_PERMISSION_FRAGMENT_TAG) as? RoomWidgetPermissionBottomSheet
                        if (dFrag != null && dFrag.dialog?.isShowing == true && !dFrag.isRemoving) {
                            return@onEach
                        } else {
                            RoomWidgetPermissionBottomSheet
                                    .newInstance(widgetArgs)
                                    .show(supportFragmentManager, WIDGET_PERMISSION_FRAGMENT_TAG)
                        }
                    }
                    WidgetStatus.WIDGET_ALLOWED -> {
                        if (supportFragmentManager.findFragmentByTag(WIDGET_FRAGMENT_TAG) == null) {
                            addFragment(views.fragmentContainer, WidgetFragment::class.java, widgetArgs, WIDGET_FRAGMENT_TAG)
                        }
                    }
                }
            }
        }

        viewModel.onEach(WidgetViewState::widgetName) { name ->
            supportActionBar?.title = name
        }

        viewModel.onEach(WidgetViewState::canManageWidgets) {
            invalidateOptionsMenu()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val widgetArgs: WidgetArgs? = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG)
        if (widgetArgs?.kind?.supportsPictureInPictureMode().orFalse()) {
            enterPictureInPicture()
        }
    }

    override fun onDestroy() {
        removeOnPictureInPictureModeChangedListener(pictureInPictureModeChangedInfoConsumer)
        super.onDestroy()
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createElementCallPipParams()?.let {
                enterPictureInPictureMode(it)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createElementCallPipParams(): PictureInPictureParams? {
        val actions = mutableListOf<RemoteAction>()
        val intent = Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_HANGUP)
        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE_HANGUP, intent, FLAG_IMMUTABLE)
        val icon = Icon.createWithResource(this, im.vector.app.R.drawable.ic_call_hangup)
        actions.add(RemoteAction(icon, getString(CommonStrings.call_notification_hangup), getString(CommonStrings.call_notification_hangup), pendingIntent))

        val aspectRatio = Rational(resources.getDimensionPixelSize(R.dimen.call_pip_width), resources.getDimensionPixelSize(R.dimen.call_pip_height))
        return PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(actions)
                .build()
    }

    private var hangupBroadcastReceiver: BroadcastReceiver? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private val pictureInPictureModeChangedInfoConsumer = Consumer<PictureInPictureModeChangedInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return@Consumer

        if (isInPictureInPictureMode) {
            hangupBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_MEDIA_CONTROL) {
                        val controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                        if (controlType == CONTROL_TYPE_HANGUP) {
                            viewModel.handle(WidgetAction.CloseWidget)
                        }
                    }
                }
            }
            ContextCompat.registerReceiver(
                    this,
                    hangupBroadcastReceiver,
                    IntentFilter(ACTION_MEDIA_CONTROL),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            unregisterReceiver(hangupBroadcastReceiver)
        }
    }

    private fun handleClose(event: WidgetViewEvents.Close) {
        if (event.content != null) {
            val intent = createResultIntent(event.content)
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }
}
