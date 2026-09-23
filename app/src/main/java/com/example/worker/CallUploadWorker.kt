package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.SmsBridgeApp

class CallUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_CALL_ID = "key_call_id"
        private const val TAG = "CallUploadWorker"
    }

    override suspend fun doWork(): Result {
        val callId = inputData.getString(KEY_CALL_ID)
        if (callId.isNullOrEmpty()) {
            Log.e(TAG, "No callId passed to CallUploadWorker")
            return Result.failure()
        }

        return try {
            val app = applicationContext as? SmsBridgeApp
            val callRepo = app?.callRepository
            if (callRepo == null) {
                Log.e(TAG, "CallRepository is null in CallUploadWorker")
                return Result.retry()
            }

            val result = callRepo.uploadPendingCall(callId)
            if (result.isSuccess) {
                Log.d(TAG, "Successfully uploaded call $callId via worker")
                Result.success()
            } else {
                Log.w(TAG, "Upload failed for call $callId; retrying with backoff...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during call upload work for $callId", e)
            Result.retry()
        }
    }
}
