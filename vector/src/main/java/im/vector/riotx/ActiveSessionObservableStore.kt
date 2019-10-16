package im.vector.riotx

import arrow.core.Option
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.core.utils.RxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionObservableStore @Inject constructor() : RxStore<Option<Session>>()
