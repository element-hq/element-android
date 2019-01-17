package im.vector.matrix.android.internal.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

object LiveDataUtils {

    fun <FIRST, SECOND, OUT> combine(firstSource: LiveData<FIRST>,
                                     secondSource: LiveData<SECOND>,
                                     mapper: (FIRST, SECOND) -> OUT): LiveData<OUT> {

        return MediatorLiveData<OUT>().apply {
            var firstValue: FIRST? = null
            var secondValue: SECOND? = null

            val valueDispatcher = {
                firstValue?.let { safeFirst ->
                    secondValue?.let { safeSecond ->
                        val mappedValue = mapper(safeFirst, safeSecond)
                        postValue(mappedValue)
                    }
                }
            }


            addSource(firstSource) {
                firstValue = it
                valueDispatcher()
            }

            addSource(secondSource) {
                secondValue = it
                valueDispatcher()
            }
        }
    }

}