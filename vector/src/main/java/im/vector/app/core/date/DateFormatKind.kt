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

    // Will show full date (Sep 7 2020)
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
