/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.share

import android.content.ClipDescription
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.attachments.AttachmentsHelper
import im.vector.riotx.features.home.LoadingFragment
import im.vector.riotx.features.home.room.list.RoomListFragment
import im.vector.riotx.features.home.room.list.RoomListParams
import im.vector.riotx.features.login.LoginActivity
import kotlinx.android.synthetic.main.activity_incoming_share.*
import javax.inject.Inject

class IncomingShareActivity :
        VectorBaseActivity(), AttachmentsHelper.Callback {

    @Inject lateinit var sessionHolder: ActiveSessionHolder
    private lateinit var roomListFragment: RoomListFragment
    private lateinit var attachmentsHelper: AttachmentsHelper

    override fun getLayoutRes(): Int {
        return R.layout.activity_incoming_share
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If we are not logged in, stop the sharing process and open login screen.
        // In the future, we might want to relaunch the sharing process after login.
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity()
            return
        }
        configureToolbar(incomingShareToolbar)
        if (isFirstCreation()) {
            val loadingDetail = LoadingFragment.newInstance()
            replaceFragment(loadingDetail, R.id.shareRoomListFragmentContainer)
        }
        attachmentsHelper = AttachmentsHelper.create(this, this).register()
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            var isShareManaged = attachmentsHelper.handleShare(intent)
            if (!isShareManaged) {
                isShareManaged = handleTextShare(intent)
            }
            if (!isShareManaged) {
                cannottManageShare()
            }
        } else {
            cannottManageShare()
        }
    }

    override fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        val roomListParams = RoomListParams(RoomListFragment.DisplayMode.SHARE, sharedData = SharedData.Attachments(attachments))
        roomListFragment = RoomListFragment.newInstance(roomListParams)
        replaceFragment(roomListFragment, R.id.shareRoomListFragmentContainer)
    }

    override fun onAttachmentsProcessFailed() {
        cannottManageShare()
    }

    private fun cannottManageShare() {
        Toast.makeText(this, R.string.error_handling_incoming_share, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun handleTextShare(intent: Intent): Boolean {
        if (intent.type == ClipDescription.MIMETYPE_TEXT_PLAIN) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            return if (sharedText.isNullOrEmpty()) {
                false
            } else {
                val roomListParams = RoomListParams(RoomListFragment.DisplayMode.SHARE, sharedData = SharedData.Text(sharedText))
                roomListFragment = RoomListFragment.newInstance(roomListParams)
                replaceFragment(roomListFragment, R.id.shareRoomListFragmentContainer)
                true
            }
        }
        return false
    }

    private fun startLoginActivity() {
        val intent = LoginActivity.newIntent(this, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
