package com.rustanovych.kidsoutfit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Entry point WorkManager instantiates by reflection, so it gets its dependencies from Koin rather
 * than a constructor. Everything of substance lives in [DailyOutfitRun].
 */
class DailyOutfitWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val run: DailyOutfitRun by inject()

    override suspend fun doWork(): Result =
        when (run.execute(attempt = runAttemptCount, isTest = inputData.getBoolean(KEY_IS_TEST, false))) {
            RunOutcome.Done -> Result.success()
            RunOutcome.Retry -> Result.retry()
        }

    companion object {
        /** Unique name of the self-rescheduling chain. */
        const val UNIQUE_NAME = "daily_outfit"

        /** Separate name so a manual test never touches the chain. */
        const val TEST_UNIQUE_NAME = "daily_outfit_test"

        const val KEY_IS_TEST = "is_test"
    }
}
