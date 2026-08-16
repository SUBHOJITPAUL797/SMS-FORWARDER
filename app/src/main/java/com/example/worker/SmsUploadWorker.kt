package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.SmsBridgeApp

class SmsUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_MESSAGE_ID = "key_message_id"
        private const val TAG = "SmsUploadWorker"
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(KEY_MESSAGE_ID)
        if (messageId.isNullOrEmpty()) {
            Log.e(TAG, "No messageId passed to SmsUploadWorker")
            return Result.failure()
        }

        return try {
            val app = applicationContext as? SmsBridgeApp
            val smsRepo = app?.smsRepository
            if (smsRepo == null) {
                Log.e(TAG, "SmsRepository is null in SmsUploadWorker")
                return Result.retry()
            }

            val result = smsRepo.uploadPendingMessage(messageId)
            if (result.isSuccess) {
                Log.d(TAG, "Successfully uploaded message $messageId via worker")
                Result.success()
            } else {
                Log.w(TAG, "Upload failed for $messageId; retrying with backoff...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload work for $messageId", e)
            Result.retry()
        }
    }
}
