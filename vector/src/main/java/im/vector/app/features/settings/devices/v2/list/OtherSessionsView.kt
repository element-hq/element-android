/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.OnModelBuildFinishedListener
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.databinding.ViewOtherSessionsBinding
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class OtherSessionsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), OtherSessionsController.Callback {

    interface Callback {
        fun onOtherSessionLongClicked(deviceId: String)
        fun onOtherSessionClicked(deviceId: String)
        fun onViewAllOtherSessionsClicked()
    }

    @Inject lateinit var otherSessionsController: OtherSessionsController

    private val views: ViewOtherSessionsBinding
    private lateinit var recyclerViewDataObserver: RecyclerView.AdapterDataObserver
    private lateinit var stateRestorer: LayoutManagerStateRestorer
    private var modelBuildListener: OnModelBuildFinishedListener? = null

    var callback: Callback? = null

    init {
        inflate(context, R.layout.view_other_sessions, this)
        views = ViewOtherSessionsBinding.bind(this)

        configureOtherSessionsRecyclerView()

        views.otherSessionsViewAllButton.setOnClickListener {
            callback?.onViewAllOtherSessionsClicked()
        }
    }

    private fun configureOtherSessionsRecyclerView() {
        views.otherSessionsRecyclerView.configureWith(otherSessionsController, hasFixedSize = false)

        val layoutManager = LinearLayoutManager(context)
        stateRestorer = LayoutManagerStateRestorer(layoutManager)
        views.otherSessionsRecyclerView.layoutManager = layoutManager
        layoutManager.recycleChildrenOnDetach = true
        modelBuildListener = OnModelBuildFinishedListener { it.dispatchTo(stateRestorer) }
        otherSessionsController.addModelBuildListener(modelBuildListener)

        recyclerViewDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                views.otherSessionsRecyclerView.scrollToPosition(0)
            }
        }
        otherSessionsController.adapter.registerAdapterDataObserver(recyclerViewDataObserver)

        otherSessionsController.callback = this
    }

    fun render(devices: List<DeviceFullInfo>, totalNumberOfDevices: Int, showViewAll: Boolean) {
        if (showViewAll) {
            views.otherSessionsViewAllButton.isVisible = true
            views.otherSessionsViewAllButton.text = context.getString(CommonStrings.device_manager_other_sessions_view_all, totalNumberOfDevices)
        } else {
            views.otherSessionsViewAllButton.isVisible = false
        }
        otherSessionsController.setData(devices)
    }

    override fun onDetachedFromWindow() {
        otherSessionsController.removeModelBuildListener(modelBuildListener)
        modelBuildListener = null
        otherSessionsController.callback = null
        otherSessionsController.adapter.unregisterAdapterDataObserver(recyclerViewDataObserver)
        views.otherSessionsRecyclerView.cleanup()
        super.onDetachedFromWindow()
    }

    override fun onItemClicked(deviceId: String) {
        callback?.onOtherSessionClicked(deviceId)
    }

    override fun onItemLongClicked(deviceId: String) {
        callback?.onOtherSessionLongClicked(deviceId)
    }
}
