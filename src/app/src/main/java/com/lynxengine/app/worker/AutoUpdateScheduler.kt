package com.lynxengine.app.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object AutoUpdateScheduler {
    private const val WORK_NAME = "lynx_auto_update"

    fun schedule(context: Context, intervalDays: Int) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<AutoUpdateWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}