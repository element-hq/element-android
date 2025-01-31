/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.widgets.WidgetPostAPIMediator
import org.matrix.android.sdk.api.session.widgets.WidgetService
import org.matrix.android.sdk.api.session.widgets.WidgetURLFormatter
import org.matrix.android.sdk.internal.session.widgets.token.DefaultGetScalarTokenTask
import org.matrix.android.sdk.internal.session.widgets.token.GetScalarTokenTask
import retrofit2.Retrofit

@Module
internal abstract class WidgetModule {

    @Module
    companion object {
        @JvmStatic
        @Provides
        fun providesWidgetsAPI(retrofit: Retrofit): WidgetsAPI {
            return retrofit.create(WidgetsAPI::class.java)
        }
    }

    @Binds
    abstract fun bindWidgetService(service: DefaultWidgetService): WidgetService

    @Binds
    abstract fun bindWidgetURLBuilder(formatter: DefaultWidgetURLFormatter): WidgetURLFormatter

    @Binds
    abstract fun bindWidgetPostAPIMediator(mediator: DefaultWidgetPostAPIMediator): WidgetPostAPIMediator

    @Binds
    abstract fun bindCreateWidgetTask(task: DefaultCreateWidgetTask): CreateWidgetTask

    @Binds
    abstract fun bindGetScalarTokenTask(task: DefaultGetScalarTokenTask): GetScalarTokenTask
}
