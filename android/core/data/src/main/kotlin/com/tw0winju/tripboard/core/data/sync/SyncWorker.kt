package com.tw0winju.tripboard.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 예전 방식이라면 Service + AsyncTask/RxJava로 짰을 백그라운드 동기화.
 * WorkManager는 프로세스가 죽거나 재부팅돼도 제약 조건(네트워크 등)이 맞으면 다시 실행해 준다.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // TODO 1. 로컬 outbox의 변경분 push
        // TODO 2. 서버에서 마지막 커서 이후 변경분 pull
        // TODO 3. core:sync 로직으로 병합 후 Room에 반영
        // TODO 실패 시 Result.retry() vs Result.failure() 기준 정하기
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "tripboard-sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
