/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.date

/* This will represent all kind of available date formats for the app.
   We will use the date Sep 7 2020 at 9:30am as an example.
   The formatting is depending of the current date.
 */
enum class DateFormatKind {
    // Will show date relative and time (today or yesterday or Sep 7 or 09/07/2020 at 9:30am)
    DEFAULT_DATE_AND_TIME,

    // Will show hour or date relative (9:30am or yesterday or Sep 7 or 09/07/2020)
    ROOM_LIST,

    // Will show full date (Sep 7, 2020)
    TIMELINE_DAY_DIVIDER,

    // Will show full date and time (Mon, Sep 7 2020, 9:30am)
    MESSAGE_DETAIL,

    // Will only show time (9:30am)
    MESSAGE_SIMPLE,

    // Will only show time (9:30am)
    EDIT_HISTORY_ROW,

    // Will only show date relative (today or yesterday or Sep 7 or 09/07/2020)
    EDIT_HISTORY_HEADER
}
