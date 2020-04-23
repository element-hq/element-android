package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.sqldelight.session.EventQueries

fun EventQueries.setDecryptionResult(result: MXEventDecryptionResult, eventId: String) {
    val decryptionResult = OlmDecryptionResult(
            payload = result.clearEvent,
            senderKey = result.senderCurve25519Key,
            keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
            forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
    )
    val adapter = MoshiProvider.providesMoshi().adapter<OlmDecryptionResult>(OlmDecryptionResult::class.java)
    val json = adapter.toJson(decryptionResult)
    setDecryptionResult(json, eventId)
}
