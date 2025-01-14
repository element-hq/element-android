/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.app.core.extensions.postLiveEvent
import im.vector.app.core.utils.LiveEvent
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toListOfPerformanceTimer
import im.vector.app.features.call.vectorCallService
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.statistics.StatisticEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionListener @Inject constructor(
        private val analyticsTracker: AnalyticsTracker
) : Session.Listener {

    private val _globalErrorLiveData = MutableLiveData<LiveEvent<GlobalError>>()
    val globalErrorLiveData: LiveData<LiveEvent<GlobalError>>
        get() = _globalErrorLiveData

    override fun onGlobalError(session: Session, globalError: GlobalError) {
        _globalErrorLiveData.postLiveEvent(globalError)
    }

    override fun onNewInvitedRoom(session: Session, roomId: String) {
        session.coroutineScope.launch {
            session.vectorCallService.userMapper.onNewInvitedRoom(roomId)
        }
    }

    override fun onStatisticsEvent(session: Session, statisticEvent: StatisticEvent) {
        statisticEvent.toListOfPerformanceTimer().forEach {
            analyticsTracker.capture(it)
        }
    }

    override fun onSessionStopped(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }

    override fun onClearCache(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }
}
