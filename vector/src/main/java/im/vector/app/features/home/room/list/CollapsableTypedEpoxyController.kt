/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        check(isBuildingModels) {
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
