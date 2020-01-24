package im.vector.matrix.android.api.session.crypto.sas

interface VerificationTransaction {


    var state: VerificationTxState

    val cancelledReason: CancelCode?
    val transactionId: String
    val otherUserId: String
    var otherDeviceId: String?
    val isIncoming: Boolean
    /**
     * User wants to cancel the transaction
     */
    fun cancel()
    fun cancel(code: CancelCode)

    fun isToDeviceTransport(): Boolean
}
