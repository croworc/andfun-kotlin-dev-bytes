/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.android.devbyteviewer

import android.os.Build
import androidx.multidex.MultiDexApplication
import androidx.work.*
import com.example.android.devbyteviewer.work.RefreshDataWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Override application to setup background work via WorkManager
 */
class DevByteApplication : MultiDexApplication() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    /**
     * onCreate is called before the first screen is shown to the user.
     *
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        delayedInit()
    }

    /**
     * Move any long-running task that has to happen upon app startup into a non-blocking
     * coroutine, so that the first screen can launch instantly
     */
    private fun delayedInit() {
        applicationScope.launch {
            // Call the method which schedules the WorkManager's job (i.e. fetching the videos from
            // the internet, once daily) from within a non-blocking coroutine
            setupRecurringWork()
        }
    }

    /**
     * Setup and enqueue the work for the WorkManager: fetching the DevByte videos from the internet,
     * once daily
     */
    private fun setupRecurringWork() {
        // Define the constraints for executing the work
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // on Wi-Fi
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setRequiresDeviceIdle(true)
                    }
                }.build()

        // Define the work request
        val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(
                1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        // Finally, schedule the work to be executed as defined
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                RefreshDataWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
        )
    }
}
