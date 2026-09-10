/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.elementx

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.settings.VectorDataStore
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session

data class MigrationBannerViewState(
        val bannerState: MigrationBannerState = MigrationBannerState.Hide
) : MavericksState

class MigrationBannerViewModel @AssistedInject constructor(
        @Assisted initialState: MigrationBannerViewState,
        session: Session,
        private val rawService: RawService,
        private val clock: Clock,
        private val vectorDataStore: VectorDataStore,
        private val stringProvider: StringProvider,
) :
        VectorViewModel<MigrationBannerViewState, MigrationBannerAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<MigrationBannerViewModel, MigrationBannerViewState> {
        override fun create(initialState: MigrationBannerViewState): MigrationBannerViewModel
    }

    companion object : MavericksViewModelFactory<MigrationBannerViewModel, MigrationBannerViewState> by hiltMavericksViewModelFactory() {
        // 7 days in milliseconds
        const val MIGRATION_BANNER_DELAY_BEFORE_SHOWING_AGAIN = 7 * 24 * 60 * 60 * 1000L

        // 15 November 2026 in milliseconds
        // Before this date, the banner will not be shown if the .well-known data is not available.
        // After this date, the banner will be shown if the .well-known data is not available.
        // If the .well-known data is available, the banner will be shown or hidden based on the data.
        const val MIGRATION_BANNER_SHOW_WHEN_NOT_CONFIGURED_START_DATE = 1794700800000L
    }

    private val sessionParams = session.sessionParams
    private val resumeFlow: MutableStateFlow<Int> = MutableStateFlow(0)

    init {
        combine(
                resumeFlow,
                vectorDataStore.elementXMigrationBannerTimestampFlow,
        ) { _, doNotShowBeforeTimestamp ->
            // Check if the banner has to be displayed or not
            val currentTime = clock.epochMillis()
            if (currentTime < doNotShowBeforeTimestamp) {
                // Banner has been closed by the user.
                MigrationBannerState.Hide
            } else {
                val useDefaultOnMissingData = currentTime >= MIGRATION_BANNER_SHOW_WHEN_NOT_CONFIGURED_START_DATE
                rawService.getElementWellknown(sessionParams)
                        .toMigrationBannerState(
                                stringProvider = stringProvider,
                                useDefaultOnMissingData = useDefaultOnMissingData,
                        )
            }
        }
                .flowOn(Dispatchers.IO)
                .setOnEach { migrationBannerState ->
                    copy(
                            bannerState = migrationBannerState,
                    )
                }
    }

    override fun handle(action: MigrationBannerAction) {
        when (action) {
            MigrationBannerAction.Close -> handleOnBannerClosed()
            MigrationBannerAction.OnResume -> handleOnResume()
        }
    }

    private fun handleOnResume() {
        resumeFlow.value++
    }

    private fun handleOnBannerClosed() {
        viewModelScope.launch {
            vectorDataStore.setElementXMigrationBannerTimestamp(clock.epochMillis() + MIGRATION_BANNER_DELAY_BEFORE_SHOWING_AGAIN)
        }
    }
}
