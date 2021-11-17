use matrix_sdk_crypto::{
    CancelInfo as RustCancelInfo, QrVerification as InnerQr, Sas as InnerSas,
    VerificationRequest as InnerVerificationRequest,
};

use crate::{OutgoingVerificationRequest, SignatureUploadRequest};

/// Enum representing the different verification flows we support.
pub enum Verification {
    /// The `m.sas.v1` verification flow.
    SasV1 {
        #[allow(missing_docs)]
        sas: Sas,
    },
    /// The `m.qr_code.scan.v1`, `m.qr_code.show.v1`, and `m.reciprocate.v1`
    /// verification flow.
    QrCodeV1 {
        #[allow(missing_docs)]
        qrcode: QrCode,
    },
}

/// The `m.sas.v1` verification flow.
pub struct Sas {
    /// The other user that is participating in the verification flow
    pub other_user_id: String,
    /// The other user's device that is participating in the verification flow
    pub other_device_id: String,
    /// The unique ID of this verification flow, will be a random string for
    /// to-device events or a event ID for in-room events.
    pub flow_id: String,
    /// The room ID where this verification is happening, will be `None` if the
    /// verification is going through to-device messages
    pub room_id: Option<String>,
    /// Did we initiate the verification flow
    pub we_started: bool,
    /// Has the non-initiating side accepted the verification flow
    pub has_been_accepted: bool,
    /// Can the short auth string be presented
    pub can_be_presented: bool,
    /// Does the flow support the emoji representation of the short auth string
    pub supports_emoji: bool,
    /// Have we confirmed that the short auth strings match
    pub have_we_confirmed: bool,
    /// Has the verification completed successfully
    pub is_done: bool,
    /// Has the flow been cancelled
    pub is_cancelled: bool,
    /// Information about the cancellation of the flow, will be `None` if the
    /// flow hasn't been cancelled
    pub cancel_info: Option<CancelInfo>,
}

/// The `m.qr_code.scan.v1`, `m.qr_code.show.v1`, and `m.reciprocate.v1`
/// verification flow.
pub struct QrCode {
    /// The other user that is participating in the verification flow
    pub other_user_id: String,
    /// The other user's device that is participating in the verification flow
    pub other_device_id: String,
    /// The unique ID of this verification flow, will be a random string for
    /// to-device events or a event ID for in-room events.
    pub flow_id: String,
    /// The room ID where this verification is happening, will be `None` if the
    /// verification is going through to-device messages
    pub room_id: Option<String>,
    /// Did we initiate the verification flow
    pub we_started: bool,
    /// Has the QR code been scanned by the other side
    pub other_side_scanned: bool,
    /// Has the scanning of the QR code been confirmed by us
    pub has_been_confirmed: bool,
    /// Did we scan the QR code and sent out a reciprocation
    pub reciprocated: bool,
    /// Has the verification completed successfully
    pub is_done: bool,
    /// Has the flow been cancelled
    pub is_cancelled: bool,
    /// Information about the cancellation of the flow, will be `None` if the
    /// flow hasn't been cancelled
    pub cancel_info: Option<CancelInfo>,
}

impl From<InnerQr> for QrCode {
    fn from(qr: InnerQr) -> Self {
        Self {
            other_user_id: qr.other_user_id().to_string(),
            flow_id: qr.flow_id().as_str().to_owned(),
            is_cancelled: qr.is_cancelled(),
            is_done: qr.is_done(),
            cancel_info: qr.cancel_info().map(|c| c.into()),
            reciprocated: qr.reciprocated(),
            we_started: qr.we_started(),
            other_side_scanned: qr.has_been_scanned(),
            has_been_confirmed: qr.has_been_confirmed(),
            other_device_id: qr.other_device_id().to_string(),
            room_id: qr.room_id().map(|r| r.to_string()),
        }
    }
}

/// Information on why a verification flow has been cancelled and by whom.
pub struct CancelInfo {
    /// The textual representation of the cancel reason
    pub reason: String,
    /// The code describing the cancel reason
    pub cancel_code: String,
    /// Was the verification flow cancelled by us
    pub cancelled_by_us: bool,
}

impl From<RustCancelInfo> for CancelInfo {
    fn from(c: RustCancelInfo) -> Self {
        Self {
            reason: c.reason().to_owned(),
            cancel_code: c.cancel_code().to_string(),
            cancelled_by_us: c.cancelled_by_us(),
        }
    }
}

/// A result type for starting SAS verifications.
pub struct StartSasResult {
    /// The SAS verification object that got created.
    pub sas: Sas,
    /// The request that needs to be sent out to notify the other side that a
    /// SAS verification should start.
    pub request: OutgoingVerificationRequest,
}

/// A result type for scanning QR codes.
pub struct ScanResult {
    /// The QR code verification object that got created.
    pub qr: QrCode,
    /// The request that needs to be sent out to notify the other side that a
    /// QR code verification should start.
    pub request: OutgoingVerificationRequest,
}

impl From<InnerSas> for Sas {
    fn from(sas: InnerSas) -> Self {
        Self {
            other_user_id: sas.other_user_id().to_string(),
            other_device_id: sas.other_device_id().to_string(),
            flow_id: sas.flow_id().as_str().to_owned(),
            is_cancelled: sas.is_cancelled(),
            is_done: sas.is_done(),
            can_be_presented: sas.can_be_presented(),
            supports_emoji: sas.supports_emoji(),
            have_we_confirmed: sas.have_we_confirmed(),
            we_started: sas.we_started(),
            room_id: sas.room_id().map(|r| r.to_string()),
            has_been_accepted: sas.has_been_accepted(),
            cancel_info: sas.cancel_info().map(|c| c.into()),
        }
    }
}

/// A result type for requesting verifications.
pub struct RequestVerificationResult {
    /// The verification request object that got created.
    pub verification: VerificationRequest,
    /// The request that needs to be sent out to notify the other side that
    /// we're requesting verification to begin.
    pub request: OutgoingVerificationRequest,
}

/// A result type for confirming verifications.
pub struct ConfirmVerificationResult {
    /// The request that needs to be sent out to notify the other side that we
    /// confirmed the verification.
    pub request: OutgoingVerificationRequest,
    /// A request that will upload signatures of the verified device or user, if
    /// the verification is completed and we're able to sign devices or users
    pub signature_request: Option<SignatureUploadRequest>,
}

/// The verificatoin request object which then can transition into some concrete
/// verification method
pub struct VerificationRequest {
    /// The other user that is participating in the verification flow
    pub other_user_id: String,
    /// The other user's device that is participating in the verification flow
    pub other_device_id: Option<String>,
    /// The unique ID of this verification flow, will be a random string for
    /// to-device events or a event ID for in-room events.
    pub flow_id: String,
    /// The room ID where this verification is happening, will be `None` if the
    /// verification is going through to-device messages
    pub room_id: Option<String>,
    /// Did we initiate the verification flow
    pub we_started: bool,
    /// Did both parties aggree to verification
    pub is_ready: bool,
    /// Did another device respond to the verification request
    pub is_passive: bool,
    /// Has the verification completed successfully
    pub is_done: bool,
    /// Has the flow been cancelled
    pub is_cancelled: bool,
    /// The list of verification methods that the other side advertised as
    /// supported
    pub their_methods: Option<Vec<String>>,
    /// The list of verification methods that we advertised as supported
    pub our_methods: Option<Vec<String>>,
    /// Information about the cancellation of the flow, will be `None` if the
    /// flow hasn't been cancelled
    pub cancel_info: Option<CancelInfo>,
}

impl From<InnerVerificationRequest> for VerificationRequest {
    fn from(v: InnerVerificationRequest) -> Self {
        Self {
            other_user_id: v.other_user().to_string(),
            other_device_id: v.other_device_id().map(|d| d.to_string()),
            flow_id: v.flow_id().as_str().to_owned(),
            is_cancelled: v.is_cancelled(),
            is_done: v.is_done(),
            is_ready: v.is_ready(),
            room_id: v.room_id().map(|r| r.to_string()),
            we_started: v.we_started(),
            is_passive: v.is_passive(),
            cancel_info: v.cancel_info().map(|c| c.into()),
            their_methods: v
                .their_supported_methods()
                .map(|v| v.into_iter().map(|m| m.to_string()).collect()),
            our_methods: v
                .our_supported_methods()
                .map(|v| v.into_iter().map(|m| m.to_string()).collect()),
        }
    }
}
