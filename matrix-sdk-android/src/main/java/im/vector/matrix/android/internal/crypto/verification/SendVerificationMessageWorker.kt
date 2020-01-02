package im.vector.matrix.android.internal.crypto.verification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.tasks.SendVerificationMessageTask
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

internal class SendVerificationMessageWorker constructor(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val userId: String,
            val event: Event
    )

    @Inject
    lateinit var sendVerificationMessageTask: SendVerificationMessageTask

    @Inject
    lateinit var cryptoService: CryptoService

    override suspend fun doWork(): Result {
        val errorOutputData = Data.Builder().putBoolean("failed", true).build()
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success(errorOutputData)

        val sessionComponent = getSessionComponent(params.userId) ?: return Result.success(errorOutputData).also {
            // TODO, can this happen? should I update local echo?
            Timber.e("Unknown Session, cannot send message, userId:${params.userId}")
        }
        sessionComponent.inject(this)
        val localId = params.event.eventId ?: ""
        return try {
            val eventId = sendVerificationMessageTask.execute(
                    SendVerificationMessageTask.Params(
                            event = params.event,
                            cryptoService = cryptoService
                    )
            )

            Result.success(Data.Builder().putString(localId, eventId).build())
        } catch (exception: Throwable) {
            if (exception.shouldBeRetried()) {
                Result.retry()
            } else {
                Result.success(errorOutputData)
            }
        }
    }

    private fun Throwable.shouldBeRetried(): Boolean {
        return this is Failure.NetworkConnection
                || (this is Failure.ServerError && error.code == MatrixError.M_LIMIT_EXCEEDED)
    }
}
