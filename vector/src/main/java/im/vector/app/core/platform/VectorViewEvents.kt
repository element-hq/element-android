/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

/**
 * Interface for View Events.
 */
interface VectorViewEvents

/**
 * To use when no view events is associated to the ViewModel.
 */
object EmptyViewEvents : VectorViewEvents
