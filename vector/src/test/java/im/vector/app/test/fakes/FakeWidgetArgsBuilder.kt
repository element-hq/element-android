/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.widgets.WidgetArgsBuilder
import io.mockk.mockk

class FakeWidgetArgsBuilder {

    val instance = mockk<WidgetArgsBuilder>()
}
