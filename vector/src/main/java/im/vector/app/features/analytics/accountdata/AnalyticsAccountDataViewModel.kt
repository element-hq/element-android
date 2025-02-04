/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.accountdata

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.log.analyticsTag
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import java.util.UUID

class AnalyticsAccountDataViewModel @AssistedInject constructor(
        @Assisted initialState: VectorDummyViewState,
        private val session: Session,
        private val analytics: VectorAnalytics
) : VectorViewModel<VectorDummyViewState, EmptyAction, EmptyViewEvents>(initialState) {

    private var checkDone: Boolean = false

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AnalyticsAccountDataViewModel, VectorDummyViewState> {
        override fun create(initialState: VectorDummyViewState): AnalyticsAccountDataViewModel
    }

    companion object : MavericksViewModelFactory<AnalyticsAccountDataViewModel, VectorDummyViewState> by hiltMavericksViewModelFactory() {
        private const val ANALYTICS_EVENT_TYPE = "im.vector.analytics"
    }

    init {
        observeAccountData()
        observeInitSync()
    }

    private fun observeInitSync() {
        combine(
                session.syncService().getSyncRequestStateFlow(),
                analytics.getUserConsent(),
                analytics.getAnalyticsId()
        ) { status, userConsent, analyticsId ->
            if (status is SyncRequestState.IncrementalSyncIdle &&
                    userConsent &&
                    analyticsId.isEmpty() &&
                    !checkDone) {
                // Initial sync is over, analytics Id from account data is missing and user has given consent to use analytics
                checkDone = true
                createAnalyticsAccountData()
            }
        }.launchIn(viewModelScope)
    }

    private fun observeAccountData() {
        session.flow()
                .liveUserAccountData(setOf(ANALYTICS_EVENT_TYPE))
                .mapNotNull { it.firstOrNull() }
                .mapNotNull { it.content.toModel<AnalyticsAccountDataContent>() }
                .onEach { analyticsAccountDataContent ->
                    if (analyticsAccountDataContent.id.isNullOrEmpty()) {
                        // Probably consent revoked from Element Web
                        // Ignore here
                        Timber.tag(analyticsTag.value).d("Consent revoked from Element Web?")
                    } else {
                        Timber.tag(analyticsTag.value).d("AnalyticsId has been retrieved")
                        analytics.setAnalyticsId(analyticsAccountDataContent.id)
                    }
                }
                .launchIn(viewModelScope)
    }

    override fun handle(action: EmptyAction) {
        // No op
    }

    private fun createAnalyticsAccountData() {
        val content = AnalyticsAccountDataContent(
                id = UUID.randomUUID().toString()
        )

        viewModelScope.launch {
            session.accountDataService().updateUserAccountData(ANALYTICS_EVENT_TYPE, content.toContent())
        }
    }
}
