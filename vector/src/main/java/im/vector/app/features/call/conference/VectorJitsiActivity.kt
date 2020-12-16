/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.call.conference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.facebook.react.modules.core.PermissionListener
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityJitsiBinding
import kotlinx.parcelize.Parcelize
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate
import org.jitsi.meet.sdk.JitsiMeetActivityInterface
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetView
import org.jitsi.meet.sdk.JitsiMeetViewListener
import org.matrix.android.sdk.api.extensions.tryOrNull
import java.net.URL
import javax.inject.Inject

class VectorJitsiActivity : VectorBaseActivity<ActivityJitsiBinding>(), JitsiMeetActivityInterface, JitsiMeetViewListener {

    @Parcelize
    data class Args(
            val roomId: String,
            val widgetId: String,
            val enableVideo: Boolean
    ) : Parcelable

    override fun getBinding() = ActivityJitsiBinding.inflate(layoutInflater)

    @Inject lateinit var viewModelFactory: JitsiCallViewModel.Factory

    private var jitsiMeetView: JitsiMeetView? = null

    private val jitsiViewModel: JitsiCallViewModel by viewModel()

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jitsiViewModel.subscribe(this) {
            renderState(it)
        }
    }

    override fun initUiAndData() {
        super.initUiAndData()
        jitsiMeetView = JitsiMeetView(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        views.jitsiLayout.addView(jitsiMeetView, params)
        jitsiMeetView?.listener = this
    }

    private fun renderState(viewState: JitsiCallViewState) {
        when (viewState.widget) {
            is Fail    -> finish()
            is Success -> {
                views.jitsiProgressLayout.isVisible = false
                jitsiMeetView?.isVisible = true
                configureJitsiView(viewState)
            }
            else       -> {
                jitsiMeetView?.isVisible = false
                views.jitsiProgressLayout.isVisible = true
            }
        }
    }

    private fun configureJitsiView(viewState: JitsiCallViewState) {
        val jitsiMeetConferenceOptions = JitsiMeetConferenceOptions.Builder()
                .setVideoMuted(!viewState.enableVideo)
                .setUserInfo(viewState.userInfo)
                .apply {
                    tryOrNull { URL(viewState.jitsiUrl) }?.let {
                        setServerURL(it)
                    }
                }
                // https://github.com/jitsi/jitsi-meet/blob/master/react/features/base/flags/constants.js
                .setFeatureFlag("chat.enabled", false)
                .setFeatureFlag("invite.enabled", false)
                .setFeatureFlag("add-people.enabled", false)
                .setFeatureFlag("video-share.enabled", false)
                .setFeatureFlag("call-integration.enabled", false)
                .setRoom(viewState.confId)
                .setSubject(viewState.subject)
                .build()
        jitsiMeetView?.join(jitsiMeetConferenceOptions)
    }

    override fun onPause() {
        JitsiMeetActivityDelegate.onHostPause(this)
        super.onPause()
    }

    override fun onResume() {
        JitsiMeetActivityDelegate.onHostResume(this)
        super.onResume()
    }

    override fun onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed()
        super.onBackPressed()
    }

    override fun onDestroy() {
        JitsiMeetActivityDelegate.onHostDestroy(this)
        super.onDestroy()
    }

//    override fun onUserLeaveHint() {
//        super.onUserLeaveHint()
//        jitsiMeetView?.enterPictureInPicture()
//    }

    override fun onNewIntent(intent: Intent?) {
        JitsiMeetActivityDelegate.onNewIntent(intent)
        super.onNewIntent(intent)
    }

    override fun requestPermissions(permissions: Array<out String>?, requestCode: Int, listener: PermissionListener?) {
        JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConferenceTerminated(p0: MutableMap<String, Any>?) {
        finish()
    }

    override fun onConferenceJoined(p0: MutableMap<String, Any>?) {
    }

    override fun onConferenceWillJoin(p0: MutableMap<String, Any>?) {
    }

    companion object {
        fun newIntent(context: Context, roomId: String, widgetId: String, enableVideo: Boolean): Intent {
            return Intent(context, VectorJitsiActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, Args(roomId, widgetId, enableVideo))
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}
