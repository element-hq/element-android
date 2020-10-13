/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.pushrules.rest.PushRule
import timber.log.Timber

sealed class Action {
    object Notify : Action()
    object DoNotNotify : Action()
    data class Sound(val sound: String = ACTION_OBJECT_VALUE_VALUE_DEFAULT) : Action()
    data class Highlight(val highlight: Boolean) : Action()

    companion object {
        const val ACTION_NOTIFY = "notify"
        const val ACTION_DONT_NOTIFY = "dont_notify"
        const val ACTION_COALESCE = "coalesce"

        // Ref: https://matrix.org/docs/spec/client_server/latest#tweaks
        const val ACTION_OBJECT_SET_TWEAK_KEY = "set_tweak"

        const val ACTION_OBJECT_SET_TWEAK_VALUE_SOUND = "sound"
        const val ACTION_OBJECT_SET_TWEAK_VALUE_HIGHLIGHT = "highlight"

        const val ACTION_OBJECT_VALUE_KEY = "value"
        const val ACTION_OBJECT_VALUE_VALUE_DEFAULT = "default"
        const val ACTION_OBJECT_VALUE_VALUE_RING = "ring"
    }
}

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#actions
 *
 * Convert
 * <pre>
 * "actions": [
 *     "notify",
 *     {
 *         "set_tweak": "sound",
 *         "value": "default"
 *     },
 *     {
 *         "set_tweak": "highlight"
 *     }
 *   ]
 *
 * To
 * [
 *     Action.Notify,
 *     Action.Sound("default"),
 *     Action.Highlight(true)
 * ]
 *
 * </pre>
 */

@Suppress("IMPLICIT_CAST_TO_ANY")
fun List<Action>.toJson(): List<Any> {
    return map { action ->
        when (action) {
            is Action.Notify      -> Action.ACTION_NOTIFY
            is Action.DoNotNotify -> Action.ACTION_DONT_NOTIFY
            is Action.Sound       -> {
                mapOf(
                        Action.ACTION_OBJECT_SET_TWEAK_KEY to Action.ACTION_OBJECT_SET_TWEAK_VALUE_SOUND,
                        Action.ACTION_OBJECT_VALUE_KEY to action.sound
                )
            }
            is Action.Highlight   -> {
                mapOf(
                        Action.ACTION_OBJECT_SET_TWEAK_KEY to Action.ACTION_OBJECT_SET_TWEAK_VALUE_HIGHLIGHT,
                        Action.ACTION_OBJECT_VALUE_KEY to action.highlight
                )
            }
        }
    }
}

fun PushRule.getActions(): List<Action> {
    val result = ArrayList<Action>()

    actions.forEach { actionStrOrObj ->
        when (actionStrOrObj) {
            Action.ACTION_NOTIFY      -> Action.Notify
            Action.ACTION_DONT_NOTIFY -> Action.DoNotNotify
            is Map<*, *>              -> {
                when (actionStrOrObj[Action.ACTION_OBJECT_SET_TWEAK_KEY]) {
                    Action.ACTION_OBJECT_SET_TWEAK_VALUE_SOUND     -> {
                        (actionStrOrObj[Action.ACTION_OBJECT_VALUE_KEY] as? String)?.let { stringValue ->
                            Action.Sound(stringValue)
                        }
                        // When the value is not there, default sound (not specified by the spec)
                                ?: Action.Sound(Action.ACTION_OBJECT_VALUE_VALUE_DEFAULT)
                    }
                    Action.ACTION_OBJECT_SET_TWEAK_VALUE_HIGHLIGHT -> {
                        (actionStrOrObj[Action.ACTION_OBJECT_VALUE_KEY] as? Boolean)?.let { boolValue ->
                            Action.Highlight(boolValue)
                        }
                        // When the value is not there, default is true, says the spec
                                ?: Action.Highlight(true)
                    }
                    else                                           -> {
                        Timber.w("Unsupported set_tweak value ${actionStrOrObj[Action.ACTION_OBJECT_SET_TWEAK_KEY]}")
                        null
                    }
                }
            }
            else                      -> {
                Timber.w("Unsupported action type $actionStrOrObj")
                null
            }
        }?.let {
            result.add(it)
        }
    }

    return result
}
