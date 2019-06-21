/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.api.pushrules

import im.vector.matrix.android.api.pushrules.rest.PushRule
import timber.log.Timber


class Action(val type: Type) {

    enum class Type(val value: String) {
        NOTIFY("notify"),
        DONT_NOTIFY("dont_notify"),
        COALESCE("coalesce"),
        SET_TWEAK("set_tweak");

        companion object {

            fun safeValueOf(value: String): Type? {
                try {
                    return valueOf(value)
                } catch (e: IllegalArgumentException) {
                    return null
                }
            }
        }
    }

    var tweak_action: String? = null
    var stringValue: String? = null
    var boolValue: Boolean? = null

    companion object {
        fun mapFrom(pushRule: PushRule): List<Action>? {
            val actions = ArrayList<Action>()
            pushRule.actions.forEach { actionStrOrObj ->
                if (actionStrOrObj is String) {
                    when (actionStrOrObj) {
                        Action.Type.NOTIFY.value      -> Action(Action.Type.NOTIFY)
                        Action.Type.DONT_NOTIFY.value -> Action(Action.Type.DONT_NOTIFY)
                        else                          -> {
                            Timber.w("Unsupported action type ${actionStrOrObj}")
                            null
                        }
                    }?.let {
                        actions.add(it)
                    }
                } else if (actionStrOrObj is Map<*, *>) {
                    val tweakAction = actionStrOrObj["set_tweak"] as? String
                    when (tweakAction) {
                        "sound"     -> {
                            (actionStrOrObj["value"] as? String)?.let { stringValue ->
                                Action(Action.Type.SET_TWEAK).also {
                                    it.tweak_action = "sound"
                                    it.stringValue = stringValue
                                    actions.add(it)
                                }
                            }
                        }
                        "highlight" -> {
                            (actionStrOrObj["value"] as? Boolean)?.let { boolValue ->
                                Action(Action.Type.SET_TWEAK).also {
                                    it.tweak_action = "highlight"
                                    it.boolValue = boolValue
                                    actions.add(it)
                                }
                            }
                        }
                        else        -> {
                            Timber.w("Unsupported action type ${actionStrOrObj}")
                        }
                    }
                } else {
                    Timber.w("Unsupported action type ${actionStrOrObj}")
                    return null
                }
            }
            return if (actions.isEmpty()) null else actions
        }
    }
}

