/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.webview

import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.weak
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber

private const val SUCCESS_URL_SUFFIX = "/_matrix/consent"
private const val RIOT_BOT_ID = "@riot-bot:matrix.org"

/**
 * This class is the Consent implementation of WebViewEventListener.
 * It is used to manage the consent agreement flow.
 */
class ConsentWebViewEventListener(
        activity: VectorBaseActivity<*>,
        private val session: Session,
        private val delegate: WebViewEventListener
) :
        WebViewEventListener by delegate {

    private val safeActivity: VectorBaseActivity<*>? by weak(activity)

    override fun onPageFinished(url: String) {
        delegate.onPageFinished(url)
        if (url.endsWith(SUCCESS_URL_SUFFIX)) {
            createRiotBotRoomIfNeeded()
        }
    }

    /**
     * This methods try to create the RiotBot room when the user gives his agreement.
     */
    private fun createRiotBotRoomIfNeeded() {
        safeActivity?.let {
            /* We do not create a Room with RiotBot in Element for the moment
            val joinedRooms = session.dataHandler.store.rooms.filter {
                it.isJoined
            }
            if (joinedRooms.isEmpty()) {
                it.showWaitingView()
                // Ensure we can create a Room with riot-bot. Error can be a MatrixError: "Federation denied with matrix.org.", or any other error.
                session.profileApiClient
                        .displayname(RIOT_BOT_ID, object : MatrixCallback<String>(createRiotBotRoomCallback) {
                            override fun onSuccess(info: String?) {
                                // Ok, the homeserver knows riot-Bot, so create a Room with him
                                session.createDirectMessageRoom(RIOT_BOT_ID, createRiotBotRoomCallback)
                            }
                        })
            } else {
             */
            it.finish()
            /*
            }
             */
        }
    }

    /**
     * APICallback instance.
     */
    private val createRiotBotRoomCallback = object : MatrixCallback<String> {
        override fun onSuccess(data: String) {
            Timber.d("## On success : succeed to invite riot-bot")
            safeActivity?.finish()
        }

        override fun onFailure(failure: Throwable) {
            Timber.e("## On error : failed  to invite riot-bot $failure")
            safeActivity?.finish()
        }
    }
}
