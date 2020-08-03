/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.rx

import androidx.paging.PagedList
import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.group.GroupSummaryQueryParams
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.room.RoomSummaryQueryParams
import im.vector.matrix.android.api.session.room.members.ChangeMembershipState
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.session.widgets.model.Widget
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.store.PrivateKeysInfo
import im.vector.matrix.android.api.session.accountdata.UserAccountDataEvent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function3

class RxSession(private val session: Session) {

    fun liveRoomSummaries(queryParams: RoomSummaryQueryParams): Observable<List<RoomSummary>> {
        return session.getRoomSummariesLive(queryParams).asObservable()
                .startWithCallable {
                    session.getRoomSummaries(queryParams)
                }
    }

    fun liveGroupSummaries(queryParams: GroupSummaryQueryParams): Observable<List<GroupSummary>> {
        return session.getGroupSummariesLive(queryParams).asObservable()
                .startWithCallable {
                    session.getGroupSummaries(queryParams)
                }
    }

    fun liveBreadcrumbs(queryParams: RoomSummaryQueryParams): Observable<List<RoomSummary>> {
        return session.getBreadcrumbsLive(queryParams).asObservable()
                .startWithCallable {
                    session.getBreadcrumbs(queryParams)
                }
    }

    fun liveMyDeviceInfo(): Observable<List<DeviceInfo>> {
        return session.cryptoService().getLiveMyDevicesInfo().asObservable()
                .startWithCallable {
                    session.cryptoService().getMyDevicesInfo()
                }
    }

    fun liveSyncState(): Observable<SyncState> {
        return session.getSyncStateLive().asObservable()
    }

    fun livePushers(): Observable<List<Pusher>> {
        return session.getPushersLive().asObservable()
    }

    fun liveUser(userId: String): Observable<Optional<User>> {
        return session.getUserLive(userId).asObservable()
                .startWithCallable {
                    session.getUser(userId).toOptional()
                }
    }

    fun liveUsers(): Observable<List<User>> {
        return session.getUsersLive().asObservable()
    }

    fun liveIgnoredUsers(): Observable<List<User>> {
        return session.getIgnoredUsersLive().asObservable()
    }

    fun livePagedUsers(filter: String? = null, excludedUserIds: Set<String>? = null): Observable<PagedList<User>> {
        return session.getPagedUsersLive(filter, excludedUserIds).asObservable()
    }

    fun liveThreePIds(refreshData: Boolean): Observable<List<ThreePid>> {
        return session.getThreePidsLive(refreshData).asObservable()
                .startWithCallable { session.getThreePids() }
    }

    fun createRoom(roomParams: CreateRoomParams): Single<String> = singleBuilder {
        session.createRoom(roomParams, it)
    }

    fun searchUsersDirectory(search: String,
                             limit: Int,
                             excludedUserIds: Set<String>): Single<List<User>> = singleBuilder {
        session.searchUsersDirectory(search, limit, excludedUserIds, it)
    }

    fun joinRoom(roomIdOrAlias: String,
                 reason: String? = null,
                 viaServers: List<String> = emptyList()): Single<Unit> = singleBuilder {
        session.joinRoom(roomIdOrAlias, reason, viaServers, it)
    }

    fun getRoomIdByAlias(roomAlias: String,
                         searchOnServer: Boolean): Single<Optional<String>> = singleBuilder {
        session.getRoomIdByAlias(roomAlias, searchOnServer, it)
    }

    fun getProfileInfo(userId: String): Single<JsonDict> = singleBuilder {
        session.getProfile(userId, it)
    }

    fun liveUserCryptoDevices(userId: String): Observable<List<CryptoDeviceInfo>> {
        return session.cryptoService().getLiveCryptoDeviceInfo(userId).asObservable().startWithCallable {
            session.cryptoService().getCryptoDeviceInfo(userId)
        }
    }

    fun liveCrossSigningInfo(userId: String): Observable<Optional<MXCrossSigningInfo>> {
        return session.cryptoService().crossSigningService().getLiveCrossSigningKeys(userId).asObservable()
                .startWithCallable {
                    session.cryptoService().crossSigningService().getUserCrossSigningKeys(userId).toOptional()
                }
    }

    fun liveCrossSigningPrivateKeys(): Observable<Optional<PrivateKeysInfo>> {
        return session.cryptoService().crossSigningService().getLiveCrossSigningPrivateKeys().asObservable()
                .startWithCallable {
                    session.cryptoService().crossSigningService().getCrossSigningPrivateKeys().toOptional()
                }
    }

    fun liveAccountData(types: Set<String>): Observable<List<UserAccountDataEvent>> {
        return session.getLiveAccountDataEvents(types).asObservable()
                .startWithCallable {
                    session.getAccountDataEvents(types)
                }
    }

    fun liveRoomWidgets(
            roomId: String,
            widgetId: QueryStringValue,
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): Observable<List<Widget>> {
        return session.widgetService().getRoomWidgetsLive(roomId, widgetId, widgetTypes, excludedTypes).asObservable()
                .startWithCallable {
                    session.widgetService().getRoomWidgets(roomId, widgetId, widgetTypes, excludedTypes)
                }
    }

    fun liveRoomChangeMembershipState(): Observable<Map<String, ChangeMembershipState>> {
        return session.getChangeMembershipsLive().asObservable()
    }

    fun liveSecretSynchronisationInfo(): Observable<SecretsSynchronisationInfo> {
        return Observable.combineLatest<List<UserAccountDataEvent>, Optional<MXCrossSigningInfo>, Optional<PrivateKeysInfo>, SecretsSynchronisationInfo>(
                liveAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME)),
                liveCrossSigningInfo(session.myUserId),
                liveCrossSigningPrivateKeys(),
                Function3 { _, crossSigningInfo, pInfo ->
                    // first check if 4S is already setup
                    val is4SSetup = session.sharedSecretStorageService.isRecoverySetup()
                    val isCrossSigningEnabled = crossSigningInfo.getOrNull() != null
                    val isCrossSigningTrusted = crossSigningInfo.getOrNull()?.isTrusted() == true
                    val allPrivateKeysKnown = pInfo.getOrNull()?.allKnown().orFalse()

                    val keysBackupService = session.cryptoService().keysBackupService()
                    val currentBackupVersion = keysBackupService.currentBackupVersion
                    val megolmBackupAvailable = currentBackupVersion != null
                    val savedBackupKey = keysBackupService.getKeyBackupRecoveryKeyInfo()

                    val megolmKeyKnown = savedBackupKey?.version == currentBackupVersion
                    SecretsSynchronisationInfo(
                            isBackupSetup = is4SSetup,
                            isCrossSigningEnabled = isCrossSigningEnabled,
                            isCrossSigningTrusted = isCrossSigningTrusted,
                            allPrivateKeysKnown = allPrivateKeysKnown,
                            megolmBackupAvailable = megolmBackupAvailable,
                            megolmSecretKnown = megolmKeyKnown,
                            isMegolmKeyIn4S = session.sharedSecretStorageService.isMegolmKeyInBackup()
                    )
                }
        )
                .distinctUntilChanged()
    }
}

fun Session.rx(): RxSession {
    return RxSession(this)
}
