package im.vector.matrix.android.api.session.crypto.sas

interface QRVerificationTransaction : VerificationTransaction {

    fun userHasScannedRemoteQrCode(scannedData: String)

}
