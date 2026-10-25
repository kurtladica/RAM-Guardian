package com.example.data.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MemoryMonitor(
    private val context: Context
) {
    private val activityManager: ActivityManager? by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    suspend fun getMemorySnapshot(): Result<MemoryInfoData> = withContext(Dispatchers.IO) {
        try {
            val am = activityManager ?: return@withContext Result.failure(
                IllegalStateException("ActivityManager system service is unavailable")
            )
            val memoryInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memoryInfo)

            val freeStorage = try {
                Environment.getDataDirectory().usableSpace
            } catch (_: Exception) {
                0L
            }

            val data = MemoryInfoData(
                totalBytes = memoryInfo.totalMem,
                availBytes = memoryInfo.availMem,
                isLowMemory = memoryInfo.lowMemory,
                lowMemThresholdBytes = memoryInfo.threshold,
                freeStorageBytes = freeStorage,
                timestampMillis = System.currentTimeMillis()
            )
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMemory(pollIntervalMillis: Long = 2000L): Flow<MemoryInfoData> = flow {
        while (currentCoroutineContext().isActive) {
            val result = getMemorySnapshot()
            result.getOrNull()?.let { emit(it) }
            delay(pollIntervalMillis)
        }
    }.flowOn(Dispatchers.IO)
}
