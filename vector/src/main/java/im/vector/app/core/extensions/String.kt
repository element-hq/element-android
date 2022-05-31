/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.extensions

private const val RTL_OVERRIDE_CHAR = '\u202E'
private const val LTR_OVERRIDE_CHAR = '\u202D'

fun String.ensureEndsLeftToRight() = if (containsRtLOverride()) "$this$LTR_OVERRIDE_CHAR" else this

fun String.containsRtLOverride() = contains(RTL_OVERRIDE_CHAR)

fun String.filterDirectionOverrides() = filterNot { it == RTL_OVERRIDE_CHAR || it == LTR_OVERRIDE_CHAR }
