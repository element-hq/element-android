package im.vector.matrix.android.internal.auth.db

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class ObjectBoxSessionParams(
        val credentialsJson: String,
        val homeServerConnectionConfigJson: String,
        @Id var id: Long = 0
)