package im.vector.matrix.android.api.session.events.interceptor

import im.vector.matrix.android.api.session.events.model.EnrichedEvent

interface EnrichedEventInterceptor {

    fun canEnrich(event: EnrichedEvent): Boolean

    fun enrich(event: EnrichedEvent)

}

