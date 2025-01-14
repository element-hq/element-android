/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.Interaction

fun Interaction.Name.toAnalyticsInteraction(interactionType: Interaction.InteractionType = Interaction.InteractionType.Touch) =
        Interaction(
                name = this,
                interactionType = interactionType
        )
