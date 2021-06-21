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

package im.vector.app.features.invite

import im.vector.app.ActiveSessionDataSource
import im.vector.app.features.session.coroutineScope
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for auto accepting invites.
 * It's listening to invites and membershipChanges so it can retry automatically if needed.
 * This mechanism will be on only if AutoAcceptInvites.isEnabled is true.
 */
@Singleton
class InvitesAcceptor @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val autoAcceptInvites: AutoAcceptInvites
) : Session.Listener {

    private lateinit var activeSessionDisposable: Disposable
    private val shouldRejectRoomIds = mutableSetOf<String>()
    private val invitedRoomDisposables = HashMap<String, Disposable>()
    private val semaphore = Semaphore(1)

    fun initialize() {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        activeSessionDisposable = sessionDataSource.observe()
                .distinctUntilChanged()
                .subscribe {
                    it.orNull()?.let { session ->
                        onSessionActive(session)
                    }
                }
    }

    private fun onSessionActive(session: Session) {
        if (!autoAcceptInvites.isEnabled) {
            return
        }
        if (invitedRoomDisposables.containsKey(session.sessionId)) {
            return
        }
        session.addListener(this)
        val roomQueryParams = roomSummaryQueryParams {
            this.memberships = listOf(Membership.INVITE)
        }
        val rxSession = session.rx()
        Observable
                .combineLatest(
                        rxSession.liveRoomSummaries(roomQueryParams),
                        rxSession.liveRoomChangeMembershipState().debounce(1, TimeUnit.SECONDS),
                        { invitedRooms, _ -> invitedRooms.map { it.roomId } }
                )
                .filter { it.isNotEmpty() }
                .subscribe { invitedRoomIds ->
                    session.coroutineScope.launch {
                        semaphore.withPermit {
                            Timber.v("Invited roomIds: $invitedRoomIds")
                            for (roomId in invitedRoomIds) {
                                async { session.joinRoomSafely(roomId) }.start()
                            }
                        }
                    }
                }
                .also {
                    invitedRoomDisposables[session.sessionId] = it
                }
    }

    private suspend fun Session.joinRoomSafely(roomId: String) {
        if (shouldRejectRoomIds.contains(roomId)) {
            getRoom(roomId)?.rejectInviteSafely()
            return
        }
        val roomMembershipChanged = getChangeMemberships(roomId)
        if (roomMembershipChanged != ChangeMembershipState.Joined && !roomMembershipChanged.isInProgress()) {
            try {
                Timber.v("Try auto join room: $roomId")
                joinRoom(roomId)
            } catch (failure: Throwable) {
                Timber.v("Failed auto join room: $roomId")
                // if we got 404 on invites, the inviting user have left or the hs is off.
                if (failure is Failure.ServerError && failure.httpCode == 404) {
                    val room = getRoom(roomId) ?: return
                    val inviterId = room.roomSummary()?.inviterId
                    // if the inviting user is on the same HS, there can only be one cause: they left, so we try to reject the invite.
                    if (inviterId?.endsWith(sessionParams.credentials.homeServer.orEmpty()).orFalse()) {
                        shouldRejectRoomIds.add(roomId)
                        room.rejectInviteSafely()
                    }
                }
            }
        }
    }

    private suspend fun Room.rejectInviteSafely() {
        try {
            leave(null)
            shouldRejectRoomIds.remove(roomId)
        } catch (failure: Throwable) {
            Timber.v("Fail rejecting invite for room: $roomId")
        }
    }

    override fun onSessionStopped(session: Session) {
        session.removeListener(this)
        invitedRoomDisposables.remove(session.sessionId)?.dispose()
    }
}
