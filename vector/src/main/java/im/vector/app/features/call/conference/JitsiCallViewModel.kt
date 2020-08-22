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

import android.net.Uri
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.asObservable
import java.net.URL

class JitsiCallViewModel @AssistedInject constructor(
        @Assisted initialState: JitsiCallViewState,
        @Assisted val args: VectorJitsiActivity.Args,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<JitsiCallViewState, JitsiCallViewActions, JitsiCallViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: JitsiCallViewState, args: VectorJitsiActivity.Args): JitsiCallViewModel
    }

    init {
        val me = session.getUser(session.myUserId)?.toMatrixItem()
        val userInfo = JitsiMeetUserInfo().apply {
            displayName = me?.getBestName()
            avatar = me?.avatarUrl?.let { session.contentUrlResolver().resolveFullSize(it) }?.let { URL(it) }
        }
        val roomName = session.getRoomSummary(args.roomId)?.displayName

        setState {
            copy(userInfo = userInfo)
        }

        session.widgetService().getRoomWidgetsLive(args.roomId, QueryStringValue.Equals(args.widgetId), WidgetType.Jitsi.values())
                .asObservable()
                .distinctUntilChanged()
                .subscribe {
                    val jitsiWidget = it.firstOrNull()
                    if (jitsiWidget != null) {
                        val uri = Uri.parse(jitsiWidget.computedUrl)
                        val confId = uri.getQueryParameter("confId")
                        val ppt = jitsiWidget.computedUrl?.let { url -> JitsiWidgetProperties(url, stringProvider) }
                        setState {
                            copy(
                                    widget = Success(jitsiWidget),
                                    jitsiUrl = "https://${ppt?.domain}",
                                    confId = confId ?: "",
                                    subject = roomName ?: ""
                            )
                        }
                    } else {
                        setState {
                            copy(
                                    widget = Fail(IllegalArgumentException("Widget not found"))
                            )
                        }
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: JitsiCallViewActions) {
    }

    companion object : MvRxViewModelFactory<JitsiCallViewModel, JitsiCallViewState> {

        const val ENABLE_VIDEO_OPTION = "ENABLE_VIDEO_OPTION"

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: JitsiCallViewState): JitsiCallViewModel? {
            val callActivity: VectorJitsiActivity = viewModelContext.activity()
            val callArgs: VectorJitsiActivity.Args = viewModelContext.args()
            return callActivity.viewModelFactory.create(state, callArgs)
        }

        override fun initialState(viewModelContext: ViewModelContext): JitsiCallViewState? {
            val args: VectorJitsiActivity.Args = viewModelContext.args()

            return JitsiCallViewState(
                    roomId = args.roomId,
                    widgetId = args.widgetId,
                    enableVideo = args.enableVideo
            )
        }
    }
}
