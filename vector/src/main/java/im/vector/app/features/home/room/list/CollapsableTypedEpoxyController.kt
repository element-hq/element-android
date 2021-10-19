/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.list

import com.airbnb.epoxy.EpoxyController

abstract class CollapsableTypedEpoxyController<T> :
    EpoxyController(), CollapsableControllerExtension {

    private var currentData: T? = null
    private var allowModelBuildRequests = false

    override var collapsed: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                allowModelBuildRequests = true
                requestModelBuild()
                allowModelBuildRequests = false
            }
        }

    fun setData(data: T?) {
        currentData = data
        allowModelBuildRequests = true
        requestModelBuild()
        allowModelBuildRequests = false
    }

    override fun requestModelBuild() {
        check(allowModelBuildRequests) {
            ("You cannot call `requestModelBuild` directly. Call `setData` instead to trigger a " +
                    "model refresh with new data.")
        }
        super.requestModelBuild()
    }

    override fun moveModel(fromPosition: Int, toPosition: Int) {
        allowModelBuildRequests = true
        super.moveModel(fromPosition, toPosition)
        allowModelBuildRequests = false
    }

    override fun requestDelayedModelBuild(delayMs: Int) {
        check(allowModelBuildRequests) {
            ("You cannot call `requestModelBuild` directly. Call `setData` instead to trigger a " +
                    "model refresh with new data.")
        }
        super.requestDelayedModelBuild(delayMs)
    }

    fun getCurrentData(): T? {
        return currentData
    }

    override fun buildModels() {
        check(isBuildingModels()) {
            ("You cannot call `buildModels` directly. Call `setData` instead to trigger a model " +
                    "refresh with new data.")
        }
        if (collapsed) {
            buildModels(null)
        } else {
            buildModels(currentData)
        }
    }

    protected abstract fun buildModels(data: T?)
}
