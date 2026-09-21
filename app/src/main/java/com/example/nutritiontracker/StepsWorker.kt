package com.example.nutritiontracker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Reads the step sensor in the background about every 15 minutes (WorkManager's minimum).
 *
 * The sensor reports a running total since the phone last restarted, so "steps today" is the
 * current total minus a baseline saved at the start of the day. StepsRepository saves that
 * baseline on the first reading of each new day. Before this worker existed, that first reading
 * only happened when the app or the widget happened to read the sensor, often late in the day,
 * and every step before it was lost. Now the day normally starts shortly after midnight, even
 * when the app is closed. Android may delay background work while the phone is idle at night,
 * but nobody is walking then, so a reading a few hours after midnight still gives a correct
 * baseline.
 */
class StepsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // Reading the steps is what saves the day's baseline (see StepsRepository).
            StepsRepository(applicationContext).getTodaySteps()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("StepsWorker", "Background step reading failed", e)
        }
        // Always report success. A missed reading isn't worth an immediate retry, since the
        // next scheduled run tries again in about 15 minutes anyway.
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "step_sensor_refresh"

        /** Safe to call every time the app starts: an existing schedule is kept as it is. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepsWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}