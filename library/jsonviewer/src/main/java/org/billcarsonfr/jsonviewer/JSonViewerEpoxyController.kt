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

package org.billcarsonfr.jsonviewer

import android.content.Context
import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.Span
import me.gujun.android.span.span

internal class JSonViewerEpoxyController(private val context: Context) :
        TypedEpoxyController<JSonViewerState>() {

    private var styleProvider: JSonViewerStyleProvider = JSonViewerStyleProvider.default(context)

    fun setStyle(styleProvider: JSonViewerStyleProvider?) {
        this.styleProvider = styleProvider ?: JSonViewerStyleProvider.default(context)
    }

    override fun buildModels(data: JSonViewerState?) {
        val async = data?.root ?: return

        when (async) {
            is Fail -> {
                valueItem {
                    id("fail")
                    text(async.error.localizedMessage?.toEpoxyCharSequence())
                }
            }
            else    -> {
                async.invoke()?.let {
                    buildRec(it, 0, "")
                }
            }
        }
    }

    private fun buildRec(
            model: JSonViewerModel,
            depth: Int,
            idBase: String
    ) {
        val host = this
        val id = "$idBase/${model.key ?: model.index}_${model.isExpanded}}"
        when (model) {
            is JSonViewerObject -> {
                if (model.isExpanded) {
                    open(id, model.key, model.index, depth, true, model)
                    model.keys.forEach {
                        buildRec(it.value, depth + 1, id)
                    }
                    close(id, depth, true)
                } else {
                    valueItem {
                        id(id + "_sum")
                        depth(depth)
                        text(
                                span {
                                    if (model.key != null) {
                                        span("\"${model.key}\"") {
                                            textColor = host.styleProvider.keyColor
                                        }
                                        span(" : ") {
                                            textColor = host.styleProvider.baseColor
                                        }
                                    }
                                    if (model.index != null) {
                                        span("${model.index}") {
                                            textColor = host.styleProvider.secondaryColor
                                        }
                                        span(" : ") {
                                            textColor = host.styleProvider.baseColor
                                        }
                                    }
                                    span {
                                        +"{+${model.keys.size}}"
                                        textColor = host.styleProvider.baseColor
                                    }
                                }.toEpoxyCharSequence()
                        )
                        itemClickListener(View.OnClickListener { host.itemClicked(model) })
                    }
                }
            }
            is JSonViewerArray  -> {
                if (model.isExpanded) {
                    open(id, model.key, model.index, depth, false, model)
                    model.items.forEach {
                        buildRec(it, depth + 1, id)
                    }
                    close(id, depth, false)
                } else {
                    valueItem {
                        id(id + "_sum")
                        depth(depth)
                        text(
                                span {
                                    if (model.key != null) {
                                        span("\"${model.key}\"") {
                                            textColor = host.styleProvider.keyColor
                                        }
                                        span(" : ") {
                                            textColor = host.styleProvider.baseColor
                                        }
                                    }
                                    if (model.index != null) {
                                        span("${model.index}") {
                                            textColor = host.styleProvider.secondaryColor
                                        }
                                        span(" : ") {
                                            textColor = host.styleProvider.baseColor
                                        }
                                    }
                                    span {
                                        +"[+${model.items.size}]"
                                        textColor = host.styleProvider.baseColor
                                    }
                                }.toEpoxyCharSequence()
                        )
                        itemClickListener(View.OnClickListener { host.itemClicked(model) })
                    }
                }
            }
            is JSonViewerLeaf   -> {
                valueItem {
                    id(id)
                    depth(depth)
                    text(
                            span {
                                if (model.key != null) {
                                    span("\"${model.key}\"") {
                                        textColor = host.styleProvider.keyColor
                                    }
                                    span(" : ") {
                                        textColor = host.styleProvider.baseColor
                                    }
                                }

                                if (model.index != null) {
                                    span("${model.index}") {
                                        textColor = host.styleProvider.secondaryColor
                                    }
                                    span(" : ") {
                                        textColor = host.styleProvider.baseColor
                                    }
                                }
                                append(host.valueToSpan(model))
                            }.toEpoxyCharSequence()
                    )
                    copyValue(model.stringRes)
                }
            }
        }
    }

    private fun valueToSpan(leaf: JSonViewerLeaf): Span {
        val host = this
        return when (leaf.type) {
            JSONType.STRING  -> {
                span("\"${leaf.stringRes}\"") {
                    textColor = host.styleProvider.stringColor
                }
            }
            JSONType.NUMBER  -> {
                span(leaf.stringRes) {
                    textColor = host.styleProvider.numberColor
                }
            }
            JSONType.BOOLEAN -> {
                span(leaf.stringRes) {
                    textColor = host.styleProvider.booleanColor
                }
            }
            JSONType.NULL    -> {
                span("null") {
                    textColor = host.styleProvider.booleanColor
                }
            }
        }
    }

    private fun open(
            id: String,
            key: String?,
            index: Int?,
            depth: Int,
            isObject: Boolean = true,
            composed: JSonViewerModel
    ) {
        val host = this
        valueItem {
            id("${id}_Open")
            depth(depth)
            text(
                    span {
                        if (key != null) {
                            span("\"$key\"") {
                                textColor = host.styleProvider.keyColor
                            }
                            span(" : ") {
                                textColor = host.styleProvider.baseColor
                            }
                        }
                        if (index != null) {
                            span("$index") {
                                textColor = host.styleProvider.secondaryColor
                            }
                            span(" : ") {
                                textColor = host.styleProvider.baseColor
                            }
                        }
                        span("- ") {
                            textColor = host.styleProvider.secondaryColor
                        }
                        span("{".takeIf { isObject } ?: "[") {
                            textColor = host.styleProvider.baseColor
                        }
                    }.toEpoxyCharSequence()
            )
            itemClickListener(View.OnClickListener { host.itemClicked(composed) })
        }
    }

    private fun itemClicked(model: JSonViewerModel) {
        model.isExpanded = !model.isExpanded
        setData(currentData)
    }

    private fun close(id: String, depth: Int, isObject: Boolean = true) {
        val host = this
        valueItem {
            id("${id}_Close")
            depth(depth)
            text(
                    span {
                        text = "}".takeIf { isObject } ?: "]"
                        textColor = host.styleProvider.baseColor
                    }.toEpoxyCharSequence()
            )
        }
    }
}
