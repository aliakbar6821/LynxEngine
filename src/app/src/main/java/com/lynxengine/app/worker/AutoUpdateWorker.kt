package com.lynxengine.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lynxengine.app.data.LynxRepository
import com.lynxengine.app.utils.ForceStopUtils
import com.lynxengine.app.utils.NetworkUtils

class AutoUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result {
        val repo = LynxRepository(applicationContext)
        if (!repo.isAutoUpdateEnabled()) return ListenableWorker.Result.success()
        if (!NetworkUtils.isOnline(applicationContext)) return ListenableWorker.Result.retry()

        return repo.downloadAndApplyFromGitHub()
            .onSuccess { ForceStopUtils.restartGoogleServices(applicationContext) }
            .fold({ ListenableWorker.Result.success() }, { ListenableWorker.Result.retry() })
    }
}