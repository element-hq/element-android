package im.vector.matrix.android.internal.auth.db

import io.realm.RealmObject

internal open class SessionParamsEntity(
        var credentialsJson: String = "",
        var homeServerConnectionConfigJson: String = ""
) : RealmObject()