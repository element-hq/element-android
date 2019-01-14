package im.vector.matrix.android.api.session.events.interceptor

import im.vector.matrix.android.api.session.events.model.TimelineEvent

interface TimelineEventInterceptor {

    fun canEnrich(event: TimelineEvent): Boolean

    fun enrich(event: TimelineEvent)

}

