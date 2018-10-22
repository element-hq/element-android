package im.vector.matrix.android.api.session.events.interceptor

import im.vector.matrix.android.api.session.events.model.EnrichedEvent

interface EnrichedEventInterceptor {

    fun enrich(roomId: String, event: EnrichedEvent)

    fun canEnrich(event: EnrichedEvent): Boolean

}

