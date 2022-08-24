package org.matrix.android.sdk.internal.database.pagedlist

import android.annotation.SuppressLint
import androidx.paging.PositionalDataSource

@Suppress("DEPRECATION")
public abstract class TiledDataSource<T> : PositionalDataSource<T>() {

  public abstract fun countItems(): Int

  public abstract fun loadRange(startPosition: Int, count: Int): List<T>?

  @SuppressLint("RestrictedApi") // For computeInitialLoadPosition, computeInitialLoadSize
  override fun loadInitial(
    params: LoadInitialParams,
    callback: LoadInitialCallback<T>
  ) {
    val totalCount = countItems()
    if (totalCount == 0) {
      callback.onResult(emptyList(), 0, 0)
      return
    }
    val firstLoadPosition = computeInitialLoadPosition(params, totalCount)
    val firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount)

    val list = loadRange(firstLoadPosition, firstLoadSize)
    if (list != null && list.size == firstLoadSize) {
      callback.onResult(list, firstLoadPosition, totalCount)
    } else {
      invalidate()
    }
  }

  override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
    val list = loadRange(params.startPosition, params.loadSize)
    if (list != null) {
      callback.onResult(list)
    } else {
      invalidate()
    }
  }
}
